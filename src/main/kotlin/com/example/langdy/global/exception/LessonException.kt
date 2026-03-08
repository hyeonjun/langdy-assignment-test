package com.example.langdy.global.exception

sealed class LessonException(
    errorType: LessonErrorType,
    customMessage: String? = null,
) : ApplicationException(errorType, customMessage)

enum class LessonErrorType(
    override val status: Int,
    override val errorCode: String,
) : ErrorType {
    SYSTEM_ERROR(500, "LS0000"),
    UNAUTHORIZED_REQUEST(400, "LS0001"),
    BAD_REQUEST(400, "LS0002"),
    ENTITY_NOT_FOUND(404, "LS0003"),
    INVALID_DATE_REQUEST(400, "LS0004"),
    ALREADY_LESSON_BOOKED(400, "LS0005"),
    DUPLICATE_LESSON(400, "LS0006"),
    WAITING_PROCESSING(400, "LS0007"),
    ;

    override fun prefix(): String = "api.lesson"
}

class LessonSystemErrorException : LessonException(LessonErrorType.SYSTEM_ERROR)
class LessonUnauthorizedException : LessonException(LessonErrorType.UNAUTHORIZED_REQUEST)
class LessonBadRequestException(message: String? = null) : LessonException(LessonErrorType.BAD_REQUEST, message)
class LessonEntityNotFoundException : LessonException(LessonErrorType.ENTITY_NOT_FOUND)
class LessonInvalidDateException : LessonException(LessonErrorType.INVALID_DATE_REQUEST)
class LessonAlreadyBookedException : LessonException(LessonErrorType.ALREADY_LESSON_BOOKED)
class LessonDuplicateException : LessonException(LessonErrorType.DUPLICATE_LESSON)
class LessonWaitingProcessingException : LessonException(LessonErrorType.WAITING_PROCESSING)