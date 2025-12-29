package com.example.authentication.data.repository

import com.example.authentication.data.local.MedTrackDatabase
import com.example.authentication.data.local.entity.UserEntity

class UserRepository(
    database: MedTrackDatabase
) {
    private val userDao = database.userDao()

    suspend fun upsertUser(uid: String, email: String, role: String) {
        userDao.upsert(UserEntity(uid = uid, email = email, role = role.lowercase()))
    }

    suspend fun getUser(uid: String): UserEntity? = userDao.getByUid(uid)

    suspend fun getPatients(): List<UserEntity> = userDao.getPatients()
}
