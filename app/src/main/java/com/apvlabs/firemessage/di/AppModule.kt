package com.apvlabs.firemessage.di

import android.content.Context
import com.apvlabs.firemessage.data.local.NotificationCache
import com.apvlabs.firemessage.data.remote.FirebaseAuthService
import com.apvlabs.firemessage.data.remote.FirestoreService
import com.apvlabs.firemessage.data.remote.NotificationApiService
import com.apvlabs.firemessage.data.repository.FcmTokenRepository
import com.apvlabs.firemessage.data.repository.NotificationRepository
import com.apvlabs.firemessage.data.repository.UserRepository
import com.apvlabs.firemessage.ui.auth.AuthViewModel
import com.apvlabs.firemessage.ui.home.HomeViewModel
import com.apvlabs.firemessage.ui.notifications.NotificationViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Módulo de inyección de dependencias con Koin
 */
val appModule = module {
    
    // Context
    single { get<Context>() }
    
    // Local
    single { NotificationCache(get()) }
    
    // Remote
    single { FirebaseAuthService() }
    single { FirestoreService() }
    single { NotificationApiService.create() }
    single { FcmTokenRepository() }
    
    // Repository
    single { UserRepository(get(), get()) }
    single { NotificationRepository(get(), get()) }
    
    // ViewModel
    viewModel { AuthViewModel(get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { NotificationViewModel(get()) }
}
