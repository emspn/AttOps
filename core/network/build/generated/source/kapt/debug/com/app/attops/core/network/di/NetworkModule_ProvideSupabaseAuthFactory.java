package com.app.attops.core.network.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.auth.Auth;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class NetworkModule_ProvideSupabaseAuthFactory implements Factory<Auth> {
  private final Provider<SupabaseClient> clientProvider;

  public NetworkModule_ProvideSupabaseAuthFactory(Provider<SupabaseClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public Auth get() {
    return provideSupabaseAuth(clientProvider.get());
  }

  public static NetworkModule_ProvideSupabaseAuthFactory create(
      Provider<SupabaseClient> clientProvider) {
    return new NetworkModule_ProvideSupabaseAuthFactory(clientProvider);
  }

  public static Auth provideSupabaseAuth(SupabaseClient client) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideSupabaseAuth(client));
  }
}
