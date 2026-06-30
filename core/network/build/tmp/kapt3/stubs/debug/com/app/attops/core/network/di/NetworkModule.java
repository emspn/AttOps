package com.app.attops.core.network.di;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\b\u0010\u0007\u001a\u00020\u0006H\u0007J\u0010\u0010\b\u001a\u00020\t2\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\f"}, d2 = {"Lcom/app/attops/core/network/di/NetworkModule;", "", "()V", "provideSupabaseAuth", "Lio/github/jan/supabase/auth/Auth;", "client", "Lio/github/jan/supabase/SupabaseClient;", "provideSupabaseClient", "provideSupabasePostgrest", "Lio/github/jan/supabase/postgrest/Postgrest;", "provideSupabaseStorage", "Lio/github/jan/supabase/storage/Storage;", "network_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class NetworkModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.app.attops.core.network.di.NetworkModule INSTANCE = null;
    
    private NetworkModule() {
        super();
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final io.github.jan.supabase.SupabaseClient provideSupabaseClient() {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final io.github.jan.supabase.auth.Auth provideSupabaseAuth(@org.jetbrains.annotations.NotNull()
    io.github.jan.supabase.SupabaseClient client) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final io.github.jan.supabase.postgrest.Postgrest provideSupabasePostgrest(@org.jetbrains.annotations.NotNull()
    io.github.jan.supabase.SupabaseClient client) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final io.github.jan.supabase.storage.Storage provideSupabaseStorage(@org.jetbrains.annotations.NotNull()
    io.github.jan.supabase.SupabaseClient client) {
        return null;
    }
}