package com.pdm.vczap_o.core.data.mock

import com.pdm.vczap_o.core.model.NewUser
import com.pdm.vczap_o.core.model.User

val CurrentUser = User(
    userId = "12345",
    username = "Eduardo",
    profileUrl = "",
    deviceToken = "PSd0239323"
)

val NewLoggedInUser = NewUser(
    userId = "12345",
    username = "Eric",
    profileUrl = "",
    deviceToken = "23434003434",
    email = "ed_129203@gmail.com"
)