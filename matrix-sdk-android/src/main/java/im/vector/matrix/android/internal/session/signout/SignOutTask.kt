/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.signout

import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.cleanup.CleanupSession
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.net.HttpURLConnection
import javax.inject.Inject

internal interface SignOutTask : Task<SignOutTask.Params, Unit> {
    data class Params(
            val signOutFromHomeserver: Boolean
    )
}

internal class DefaultSignOutTask @Inject constructor(
        private val signOutAPI: SignOutAPI,
        private val eventBus: EventBus,
        private val cleanupSession: CleanupSession
) : SignOutTask {

    override suspend fun execute(params: SignOutTask.Params) {
        // It should be done even after a soft logout, to be sure the deviceId is deleted on the
        if (params.signOutFromHomeserver) {
            Timber.d("SignOut: send request...")
            try {
                executeRequest<Unit>(eventBus) {
                    apiCall = signOutAPI.signOut()
                }
            } catch (throwable: Throwable) {
                // Maybe due to https://github.com/matrix-org/synapse/issues/5755
                if (throwable is Failure.ServerError
                        && throwable.httpCode == HttpURLConnection.HTTP_UNAUTHORIZED /* 401 */
                        && throwable.error.code == MatrixError.M_UNKNOWN_TOKEN) {
                    // Also throwable.error.isSoftLogout should be true
                    // Ignore
                    Timber.w("Ignore error due to https://github.com/matrix-org/synapse/issues/5755")
                } else {
                    throw throwable
                }
            }
        }

        Timber.d("SignOut: cleanup session...")
        cleanupSession.handle()
    }
}
