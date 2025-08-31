package com.pdm.vczap_o.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.pdm.vczap_o.chatRoom.data.local.ChatDatabase
import com.pdm.vczap_o.chatRoom.data.local.MessageDao
import com.pdm.vczap_o.core.data.dataStore
import com.pdm.vczap_o.cripto.CryptoService
import com.pdm.vczap_o.group.data.GroupRepository
import com.pdm.vczap_o.group.domain.usecase.AddMemberUseCase
import com.pdm.vczap_o.group.domain.usecase.CreateGroupUseCase
import com.pdm.vczap_o.group.domain.usecase.GetGroupsUseCase
import com.pdm.vczap_o.group.domain.usecase.RemoveMemberUseCase
import com.pdm.vczap_o.home.data.SearchUsersRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Providers de Firebase & Context ---
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext appContext: Context): Context = appContext

    // --- Providers de Base de Dados & DataStore ---
    @Provides
    @Singleton
    fun provideMessageDao(@ApplicationContext appContext: Context): MessageDao =
        ChatDatabase.getDatabase(appContext).messageDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> =
        appContext.applicationContext.dataStore

    // --- Providers de Serviços ---
    @Provides
    @Singleton
    fun provideCryptoService(@ApplicationContext appContext: Context): CryptoService =
        CryptoService(appContext)

    // --- Providers de Repositórios ---
    @Provides
    @Singleton
    fun provideSearchUsersRepository(firestore: FirebaseFirestore): SearchUsersRepository =
        SearchUsersRepository(firestore)

    @Provides
    @Singleton
    fun providesGroupRepository(firestore: FirebaseFirestore): GroupRepository =
        GroupRepository(firestore)

    // --- Providers de Casos de Uso (UseCases) ---
    @Provides
    @Singleton
    fun providesCreateGroupUseCase(groupRepository: GroupRepository): CreateGroupUseCase =
        CreateGroupUseCase(groupRepository)

    @Provides
    @Singleton
    fun providesAddMemberUseCase(groupRepository: GroupRepository): AddMemberUseCase =
        AddMemberUseCase(groupRepository)

    @Provides
    @Singleton
    fun providesRemoveMemberUseCase(groupRepository: GroupRepository): RemoveMemberUseCase =
        RemoveMemberUseCase(groupRepository)

    @Provides
    @Singleton
    fun providesGetGroupsUseCase(groupRepository: GroupRepository): GetGroupsUseCase =
        GetGroupsUseCase(groupRepository)

}

