package com.derived.campusdesk.di

import android.content.Context
import com.derived.campusdesk.BuildConfig
import com.derived.campusdesk.auth.SessionStore
import com.derived.campusdesk.attendance.FusedLocationProvider
import com.derived.campusdesk.attendance.LocationProvider
import com.derived.campusdesk.networking.api.ApiConfigProvider
import com.derived.campusdesk.networking.api.ApiConfiguration
import com.derived.campusdesk.networking.api.ApiEnvironmentPreset
import com.derived.campusdesk.networking.api.ApiEnvironmentStore
import com.derived.campusdesk.networking.api.CampusDeskApi
import com.derived.campusdesk.networking.client.NetworkClientFactory
import com.derived.campusdesk.networking.analytics.ArcherApiInterceptor
import com.derived.campusdesk.networking.debug.DevToolsConfig
import com.derived.campusdesk.networking.debug.NetworkResponseLogger
import com.derived.campusdesk.networking.security.PasswordEncryptor
import com.derived.campusdesk.networking.services.AttendanceService
import com.derived.campusdesk.networking.services.AttendanceServiceImpl
import com.derived.campusdesk.networking.services.AuthService
import com.derived.campusdesk.networking.services.AuthServiceImpl
import com.derived.campusdesk.networking.services.CampusService
import com.derived.campusdesk.networking.services.CampusServiceImpl
import com.derived.campusdesk.networking.services.StudentService
import com.derived.campusdesk.networking.services.StudentServiceImpl
import com.derived.campusdesk.networking.storage.SecureTokenStore
import com.derived.campusdesk.networking.storage.TokenStore
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
    fun provideDevToolsConfig(config: AppDevToolsConfig): DevToolsConfig = config

    @Provides
    @Singleton
    fun provideApiEnvironmentStore(@ApplicationContext context: Context): ApiEnvironmentStore =
        SharedPrefsApiEnvironmentStore(context)

    @Provides
    @Singleton
    fun provideApiConfigProvider(
        environmentStore: ApiEnvironmentStore,
    ): ApiConfigProvider = AppApiConfigProvider(environmentStore)

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore =
        SecureTokenStore(context)

    @Provides
    @Singleton
    fun provideNetworkClientFactory(
        apiConfigProvider: ApiConfigProvider,
        tokenStore: TokenStore,
        devToolsConfig: DevToolsConfig,
    ): NetworkClientFactory {
        val logger = if (devToolsConfig.isDevToolsEnabled) NetworkResponseLogger(devToolsConfig) else null
        return NetworkClientFactory(
            apiConfigProvider = apiConfigProvider,
            tokenStore = tokenStore,
            debugInterceptor = logger,
            analyticsInterceptor = ArcherApiInterceptor(),
        )
    }

    @Provides
    @Singleton
    fun provideCampusDeskApi(factory: NetworkClientFactory): CampusDeskApi = factory.createApi()

    @Provides
    @Singleton
    fun providePasswordEncryptor(factory: NetworkClientFactory): PasswordEncryptor =
        PasswordEncryptor({ factory.createApi() })

    @Provides
    @Singleton
    fun provideAuthService(
        factory: NetworkClientFactory,
        apiConfigProvider: ApiConfigProvider,
        encryptor: PasswordEncryptor,
    ): AuthService = AuthServiceImpl(
        { factory.createApi() },
        factory.campusJson,
        apiConfigProvider,
        encryptor,
    )

    @Provides
    @Singleton
    fun provideCampusService(
        factory: NetworkClientFactory,
    ): CampusService = CampusServiceImpl({ factory.createApi() }, factory.campusJson)

    @Provides
    @Singleton
    fun provideAttendanceService(
        factory: NetworkClientFactory,
    ): AttendanceService = AttendanceServiceImpl({ factory.createApi() }, factory.campusJson)

    @Provides
    @Singleton
    fun provideStudentService(
        factory: NetworkClientFactory,
    ): StudentService = StudentServiceImpl({ factory.createApi() }, factory.campusJson)

    @Provides
    @Singleton
    fun provideSessionStore(authService: AuthService, tokenStore: TokenStore): SessionStore =
        SessionStore(authService, tokenStore)

    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider =
        FusedLocationProvider(context)
}

private class AppApiConfigProvider(
    private val environmentStore: ApiEnvironmentStore,
) : ApiConfigProvider {
    override fun current(): ApiConfiguration {
        // Debug + env picker: respect an explicit saved preset (Production / Development).
        if (BuildConfig.DEBUG && BuildConfig.DEV_TOOLS && environmentStore.hasSavedSelection) {
            return environmentStore.selected.configuration
        }
        // Debug default: BuildConfig API_BASE_URL (adequate-success Railway).
        if (BuildConfig.DEBUG) {
            return ApiConfiguration(
                baseUrl = ApiConfiguration.normalizedBaseUrl(BuildConfig.API_BASE_URL),
                timeoutSeconds = 30,
                environment = com.derived.campusdesk.networking.api.AppEnvironment.DEVELOPMENT,
            )
        }
        // Release: always production host from BuildConfig.
        return ApiConfiguration(
            baseUrl = ApiConfiguration.normalizedBaseUrl(BuildConfig.API_BASE_URL),
            timeoutSeconds = 30,
            environment = com.derived.campusdesk.networking.api.AppEnvironment.PRODUCTION,
        )
    }

    override fun defaultInstituteSlug(): String = BuildConfig.INSTITUTE_SLUG
}

private class SharedPrefsApiEnvironmentStore(
    context: Context,
) : ApiEnvironmentStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        // One-shot: drop a stale "Production" pick that pointed devices at a host
        // some networks can't resolve, so debug boots on adequate-success again.
        if (prefs.getInt(MIGRATION, 0) < 1) {
            prefs.edit()
                .remove(KEY)
                .putInt(MIGRATION, 1)
                .putString(KEY, ApiEnvironmentPreset.Development.label)
                .commit()
        }
    }

    override val hasSavedSelection: Boolean
        get() = prefs.contains(KEY)

    override var selected: ApiEnvironmentPreset
        get() {
            val raw = prefs.getString(KEY, ApiEnvironmentPreset.Development.label)
                ?: ApiEnvironmentPreset.Development.label
            return ApiEnvironmentPreset.entries.firstOrNull { it.label == raw }
                ?: ApiEnvironmentPreset.Development
        }
        set(value) {
            prefs.edit().putString(KEY, value.label).commit()
        }

    override fun clearSavedSelection() {
        prefs.edit().remove(KEY).commit()
    }

    companion object {
        private const val PREFS = "campusdesk.api"
        private const val KEY = "campusdesk.api.environment"
        private const val MIGRATION = "campusdesk.api.environment.migration"
    }
}
