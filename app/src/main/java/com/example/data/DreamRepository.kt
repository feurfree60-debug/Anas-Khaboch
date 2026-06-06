package com.example.data

import kotlinx.coroutines.flow.Flow

class DreamRepository(private val dreamDao: DreamDao) {
    val allDreams: Flow<List<DreamEntity>> = dreamDao.getAllDreams()

    suspend fun getDreamById(id: Int): DreamEntity? {
        return dreamDao.getDreamById(id)
    }

    suspend fun insertDream(dream: DreamEntity): Long {
        return dreamDao.insertDream(dream)
    }

    suspend fun updateDream(dream: DreamEntity) {
        dreamDao.updateDream(dream)
    }

    suspend fun deleteDream(dream: DreamEntity) {
        dreamDao.deleteDream(dream)
    }

    suspend fun deleteDreamById(id: Int) {
        dreamDao.deleteDreamById(id)
    }
}
