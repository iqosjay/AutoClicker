import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
}

val signingDirectory = file(providers.gradleProperty("signingDirectory").orElse("D:/SDK/AndroidSignatures").get())
val signingProperties = Properties().apply {
  signingDirectory.resolve("roy.txt").reader(Charsets.UTF_8).use {
    load(it)
  }
}
val signingValues = listOf("STORE_PASSWORD", "ALIAS", "KEY_PASSWORD").associateWith { key ->
  requireNotNull(signingProperties.getProperty(key)?.takeIf { it.isNotBlank() }) {
    "签名信息 roy.txt 缺少 $key"
  }
}

android {
  namespace = "com.roy.autoclicker"
  compileSdk {
    version = release(37)
  }

  defaultConfig {
    applicationId = "com.roy.autoclicker"
    minSdk = 24
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
  }

  signingConfigs {
    create("roy") {
      storeFile = signingDirectory.resolve("roy.jks")
      storePassword = signingValues.getValue("STORE_PASSWORD")
      keyAlias = signingValues.getValue("ALIAS")
      keyPassword = signingValues.getValue("KEY_PASSWORD")
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("roy")
    }
    release {
      signingConfig = signingConfigs.getByName("roy")
      optimization {
        enable = false
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}
