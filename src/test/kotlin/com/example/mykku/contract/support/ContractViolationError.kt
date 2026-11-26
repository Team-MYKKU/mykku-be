package com.example.mykku.contract.support

class ContractViolationError(message: String, cause: Throwable? = null) : AssertionError(message, cause)
