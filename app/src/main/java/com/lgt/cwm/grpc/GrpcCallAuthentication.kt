package com.lgt.cwm.grpc

import io.grpc.CallCredentials
import io.grpc.Metadata
import io.grpc.Status
import java.util.concurrent.Executor

class GrpcCallAuthentication(val authorization: String, val phoneFull: String): CallCredentials(){
    override fun applyRequestMetadata(
        requestInfo: RequestInfo?,
        appExecutor: Executor?,
        applier: MetadataApplier?
    ) {
        appExecutor?.execute {
            try {
                val headers = Metadata()
                headers.put(GRPCAuthField.AUTHORIZATION_METADATA_AUTH_KEY, this.authorization)
                headers.put(GRPCAuthField.AUTHORIZATION_METADATA_AUTH_CLIENT, this.phoneFull)

                applier?.apply(headers)
            } catch (e: Throwable) {
                applier?.fail(Status.UNAUTHENTICATED.withCause(e))
            }
        }
    }

    override fun thisUsesUnstableApi() {

    }
}