package com.example.echowithin.presentation.navigation

object AppRoute {
    const val Welcome = "welcome"
    const val Login = "login"
    const val Register = "register"
    const val ConfirmEmail = "confirm_email/{email}"
    const val Home = "home"
    const val Search = "search"
    const val Premium = "premium"
    const val Settings = "settings"
    const val Trash = "trash"
    const val AppLock = "app_lock"
    const val Editor = "editor"
    const val EditorWithArg = "editor?noteId={noteId}"
    const val Detail = "detail/{noteId}"
    const val Share = "share/{noteId}"
    const val Versions = "versions/{noteId}"

    fun editor(noteId: String?): String =
        if (noteId.isNullOrBlank()) Editor else "editor?noteId=$noteId"

    fun detail(noteId: String): String = "detail/$noteId"
    fun share(noteId: String): String = "share/$noteId"
    fun versions(noteId: String): String = "versions/$noteId"
    fun confirmEmail(email: String): String = "confirm_email/$email"
}
