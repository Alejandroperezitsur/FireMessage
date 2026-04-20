package com.apvlabs.firemessage.data.remote

import com.apvlabs.firemessage.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Servicio de base de datos Firestore
 */
class FirestoreService {
    
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    
    /**
     * Guardar usuario en Firestore
     */
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener usuario por ID
     */
    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Actualizar token FCM del usuario
     */
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("fcmToken", token, "updatedAt", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener usuarios por rol
     */
    suspend fun getUsersByRole(role: String): Result<List<User>> {
        return try {
            val snapshot = usersCollection.whereEqualTo("role", role).get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener usuarios por carrera
     */
    suspend fun getUsersByCareer(career: String): Result<List<User>> {
        return try {
            val snapshot = usersCollection.whereEqualTo("career", career).get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener todos los usuarios
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersCollection.get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener usuarios por rol y carrera
     */
    suspend fun getUsersByRoleAndCareer(role: String, career: String): Result<List<User>> {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("role", role)
                .whereEqualTo("career", career)
                .get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
