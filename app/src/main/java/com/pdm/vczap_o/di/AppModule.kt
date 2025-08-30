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
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext appContext: Context): Context {
        return appContext
    }

    @Provides
    @Singleton
    fun provideMessageDao(@ApplicationContext appContext: Context): MessageDao {
        return ChatDatabase.getDatabase(appContext).messageDao()
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return appContext.applicationContext.dataStore
    }

    @Provides
    @Singleton
    fun provideCryptoService(@ApplicationContext appContext: Context): CryptoService {
        return CryptoService(appContext)
    }


    // ------------------ GRUPOS ------------------
    @Provides
    @Singleton
    fun providesGroupRepository(
        firestore: FirebaseFirestore
    ): GroupRepository {
        return GroupRepository(firestore)
    }

    @Provides
    @Singleton
    fun providesCreateGroupUseCase(
        groupRepository: GroupRepository
    ): CreateGroupUseCase {
        return CreateGroupUseCase(groupRepository)
    }

    @Provides
    @Singleton
    fun providesAddMemberUseCase(
        groupRepository: GroupRepository
    ): AddMemberUseCase {
        return AddMemberUseCase(groupRepository)
    }

    @Provides
    @Singleton
    fun providesRemoveMemberUseCase(
        groupRepository: GroupRepository
    ): RemoveMemberUseCase {
        return RemoveMemberUseCase(groupRepository)
    }

    @Provides
    @Singleton
    fun providesGetGroupsUseCase(
        groupRepository: GroupRepository
    ): GetGroupsUseCase {
        return GetGroupsUseCase(groupRepository)
    }
    // ------------------------------------------
}

