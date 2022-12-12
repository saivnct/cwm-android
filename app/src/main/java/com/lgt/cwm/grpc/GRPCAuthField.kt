package com.lgt.cwm.grpc
import io.grpc.Metadata
import io.grpc.Metadata.ASCII_STRING_MARSHALLER




/**
 * Created by giangtpu on 6/29/22.
 */
object GRPCAuthField {
    val AUTHORIZATION_METADATA_AUTH_KEY: Metadata.Key<String> =
        Metadata.Key.of("authorization", ASCII_STRING_MARSHALLER)
    val AUTHORIZATION_METADATA_AUTH_CLIENT: Metadata.Key<String> =
        Metadata.Key.of("client", ASCII_STRING_MARSHALLER)

}