package com.lgt.cwm.grpc

import android.content.Context
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.util.Config
import io.grpc.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.concurrent.TimeUnit


object GrpcUtils {

    fun getChannel(context: Context, acc: Account? = null): ManagedChannel? {
        var channel: ManagedChannel? = null
        return try {
            val authorization = acc?.jwt ?: ""
            val phoneFull = acc?.phoneFull ?: ""

            if (Config.GRPC.TLS){
                val target = Config.GRPC.HOST+":"+Config.GRPC.PORT

                //https://grpc.io/docs/guides/auth/
                val am = context.assets
                val inputStream = am.open(Config.GRPC.TLS_CERT_FILE_NAME)
                val tlsChannelCredentials = TlsChannelCredentials
                                            .newBuilder()
                                            .trustManager(inputStream)
                                            .build()

                val creds: ChannelCredentials
                if (Config.GRPC.CHANNEL_CREDENTIAL_AUTHENTICATION){
                    val grpcCallAuthentication = GrpcCallAuthentication(
                        authorization = authorization,
                        phoneFull = phoneFull
                    )

                    //https://sultanov.dev/blog/securing-java-grpc-services-with-jwt-based-authentication/
                    creds = CompositeChannelCredentials.create(
                        tlsChannelCredentials,
                        grpcCallAuthentication
                    )
                }else{
                    creds = tlsChannelCredentials
                }

                channel = Grpc.newChannelBuilder(target, creds)
                    .idleTimeout(30, TimeUnit.SECONDS)
                    .executor(Dispatchers.IO.asExecutor())
                    .build()
            }else{
                val creds: ChannelCredentials
                if (Config.GRPC.CHANNEL_CREDENTIAL_AUTHENTICATION){
                    val grpcCallAuthentication = GrpcCallAuthentication(
                        authorization = authorization,
                        phoneFull = phoneFull
                    )
                    //https://sultanov.dev/blog/securing-java-grpc-services-with-jwt-based-authentication/
                    creds = CompositeChannelCredentials.create(
                        InsecureChannelCredentials.create(),
                        grpcCallAuthentication
                    )

                    val target = Config.GRPC.HOST+":"+Config.GRPC.PORT
                    channel = Grpc.newChannelBuilder(target, creds)
                        .idleTimeout(Config.GRPC.TIMEOUT, TimeUnit.SECONDS)
                        .executor(Dispatchers.IO.asExecutor())
                        .build()
                }else{
                    channel = ManagedChannelBuilder
                        .forAddress(Config.GRPC.HOST, Config.GRPC.PORT)
                        .idleTimeout(Config.GRPC.TIMEOUT, TimeUnit.SECONDS)
                        .usePlaintext()
                        .executor(Dispatchers.IO.asExecutor())
                        .build()
                }
            }
            channel
        } catch (ex: Exception) {
            ex.printStackTrace()
            channel
        }

    }
}