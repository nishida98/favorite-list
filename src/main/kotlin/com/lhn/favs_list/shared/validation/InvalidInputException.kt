package com.lhn.favs_list.shared.validation

class InvalidInputException(
    val fieldErrors: List<FieldValidationError>,
    message: String = "Invalid input",
) : RuntimeException(message) {
    init {
        require(fieldErrors.isNotEmpty()) {
            "fieldErrors must not be empty"
        }
    }
}
