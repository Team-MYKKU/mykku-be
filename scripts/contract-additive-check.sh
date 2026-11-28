#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

usage() {
  echo "사용: scripts/contract-additive-check.sh <기준 브랜치> [승인 블록 키 목록] [새 파일 허용 접두 목록] [승인된 기존 파일 목록]"
  echo "  기준 브랜치(예: origin/develop)와 HEAD의 merge-base를 기준으로 커밋된 contract 골든 변경이 '선언한 추가만'인지 본다."
  echo "  목록 파일은 한 줄에 하나다. 주지 않으면 빈 목록이다. 골든 갱신을 커밋한 뒤 깨끗한 작업 트리에서 돌린다."
}

absolute() {
  if [ "$1" = "/dev/null" ]; then echo /dev/null; else realpath -- "$1"; fi
}

[ $# -ge 1 ] || { usage; exit 2; }
BASE_REF="$1"
ALLOWED_BLOCKS="$(absolute "${2:-/dev/null}")"
NEW_PREFIXES="$(absolute "${3:-/dev/null}")"
APPROVED_FILES="$(absolute "${4:-/dev/null}")"
cd "$(git rev-parse --show-toplevel)"
C=src/test/resources/contract
M="$C/MANIFEST.sha256"
ALLOW='^(wire/endpoints/(app|admin)\.txt|wire/schema/(response|request)\.txt|wire/matrix/(auth|not-found|empty-state|pagination)\.txt|wire/error-codes/(list\.http|by-domain-event\.http|responses\.txt)|wire/snippets/error-code/list/200/http-response\.adoc)$'
fail=0

command -v jq >/dev/null || { echo "jq가 필요하다"; exit 2; }
git rev-parse --verify -q "$BASE_REF^{commit}" >/dev/null || { echo "기준을 찾을 수 없다: $BASE_REF"; exit 2; }
if ! git diff --quiet HEAD -- "$C" || [ -n "$(git ls-files --others --exclude-standard -- "$C")" ]; then
  echo "contract 아래에 커밋하지 않은 변경이 있다. 골든 갱신을 커밋한 뒤 돌린다"
  exit 2
fi
MB="$(git merge-base HEAD "$BASE_REF")"
R="$MB..HEAD"
T="$(mktemp -d)"
trap 'rm -rf "$T"' EXIT

listed() {
  grep -Fxq -- "$1" "$2"
}

under_new_prefix() {
  local rel="$1" prefix
  while IFS= read -r prefix; do
    if [ -n "$prefix" ] && [ "${rel#"$prefix"}" != "$rel" ]; then return 0; fi
  done < "$NEW_PREFIXES"
  return 1
}

check_file_status() {
  local st path rel
  git diff --no-renames --name-status "$R" -- "$C" > "$T/status"
  while IFS=$'\t' read -r st path; do
    rel="${path#"$C"/}"
    case "$st" in
      A) under_new_prefix "$rel" || listed "$rel" "$APPROVED_FILES" || { echo "선언하지 않은 위치의 새 파일: $rel"; fail=1; } ;;
      M) case "$rel" in
           fixtures/*|docs/*) listed "$rel" "$APPROVED_FILES" || { echo "기존 파일 수정 금지: $rel"; fail=1; } ;;
         esac ;;
      *) echo "금지($st): $rel"; fail=1 ;;
    esac
  done < "$T/status"
}

check_modified_lines() {
  local add del path rel
  git diff --minimal --no-renames --numstat --diff-filter=M "$R" -- "$C" > "$T/numstat"
  while IFS=$'\t' read -r add del path; do
    rel="${path#"$C"/}"
    [ "$rel" = "MANIFEST.sha256" ] && continue
    listed "$rel" "$APPROVED_FILES" && continue
    if ! grep -Eq "$ALLOW" <<<"$rel"; then echo "허용 목록 밖 수정: $rel"; fail=1; continue; fi
    [ "$del" = "0" ] || { echo "기존 줄 삭제·변경: $rel (-$del)"; fail=1; }
  done < "$T/numstat"
}

blocks_of() {
  git show "$1:$C/$2" | awk 'BEGIN{RS="";ORS="\0"}{print}' | sort -z
}

check_schema_blocks() {
  local f block key
  for f in wire/schema/response.txt wire/schema/request.txt; do
    listed "$f" "$APPROVED_FILES" && continue
    blocks_of "$MB" "$f" > "$T/old.b"
    blocks_of HEAD "$f" > "$T/new.b"
    comm -z -23 "$T/old.b" "$T/new.b" > "$T/gone.b"
    while IFS= read -r -d '' block; do
      key="${block%%$'\n'*}"
      listed "$key" "$ALLOWED_BLOCKS" && continue
      echo "기존 블록 변경: $f :: $key"
      fail=1
    done < "$T/gone.b"
  done
}

elements_of() {
  git show "$1:$C/$2" | sed '1,/^$/d' | jq -c '.data[]' | sort
}

check_error_code_elements() {
  local f
  for f in wire/error-codes/list.http wire/error-codes/by-domain-event.http; do
    elements_of "$MB" "$f" > "$T/old.j"
    elements_of HEAD "$f" > "$T/new.j"
    comm -23 "$T/old.j" "$T/new.j" > "$T/gone.j"
    if [ -s "$T/gone.j" ]; then echo "기존 에러 코드 원소 변경: $f"; cat "$T/gone.j"; fail=1; fi
  done
}

changed_paths() {
  git diff --no-renames --name-only --diff-filter="$1" "$R" -- "$C" | sed "s#^$C/##" | { grep -vx 'MANIFEST.sha256' || true; } | sort
}

manifest_paths() {
  git diff -U0 "$R" -- "$M" | { grep -E "^$1[0-9a-f]{64}  " || true; } | sed -E "s/^$1[0-9a-f]{64}  //" | sort
}

check_manifest() {
  [ "$(git show "HEAD:$M" | head -n1)" = "base $MB" ] || { echo "MANIFEST base가 merge-base($MB)와 다르다"; fail=1; }
  changed_paths M > "$T/mod"
  changed_paths AM > "$T/all"
  manifest_paths '-' > "$T/del"
  manifest_paths '\+' > "$T/add"
  cmp -s "$T/del" "$T/mod" || { echo "MANIFEST: 지운 해시 줄과 수정 파일이 다르다"; diff "$T/del" "$T/mod" || true; fail=1; }
  cmp -s "$T/add" "$T/all" || { echo "MANIFEST: 더한 해시 줄과 수정·새 파일이 다르다"; diff "$T/add" "$T/all" || true; fail=1; }
}

check_file_status
check_modified_lines
check_schema_blocks
check_error_code_elements
check_manifest

echo "기준: $BASE_REF (merge-base $MB), 대상: $R"
git diff --no-renames --numstat "$R" -- "$C"
if [ "$fail" = 0 ]; then echo "추가만 검사 통과"; else echo "추가만 검사 실패"; fi
exit "$fail"
