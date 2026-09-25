package com.example.mykku.image

interface ImageCleanupPort {
    fun deleteAfterCommit(urls: Collection<String>)
}
