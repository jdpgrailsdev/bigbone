package social.bigbone.rx

import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import social.bigbone.MastodonClient
import social.bigbone.api.Handler
import social.bigbone.api.Shutdownable
import social.bigbone.api.entity.Notification
import social.bigbone.api.entity.Status
import social.bigbone.api.method.StreamingMethods

class RxStreamingMethods(client: MastodonClient) {

    val streamingMethods = StreamingMethods(client)

    private fun stream(f: (Handler) -> Shutdownable): Flowable<Status> {
        return Flowable.create<Status>({ emmiter ->
            val shutdownable = f(object : Handler {
                override fun onStatus(status: Status) {
                    emmiter.onNext(status)
                }

                override fun onNotification(notification: Notification) {
                    // no op
                }

                override fun onDelete(id: String) {
                    // no op
                }
            })
            emmiter.setCancellable {
                shutdownable.shutdown()
            }
        }, BackpressureStrategy.LATEST)
    }

    private fun statusStream(f: (Handler) -> Shutdownable): Flowable<Status> {
        return stream { handler ->
            f(handler)
        }
    }

    private fun tagStream(tag: String, f: (String, Handler) -> Shutdownable): Flowable<Status> {
        return stream { handler ->
            f(tag, handler)
        }
    }

    fun localPublic(): Flowable<Status> = statusStream(streamingMethods::localPublic)

    fun federatedPublic(): Flowable<Status> = statusStream(streamingMethods::federatedPublic)

    fun localHashtag(tag: String): Flowable<Status> = tagStream(tag, streamingMethods::localHashtag)

    fun federatedHashtag(tag: String): Flowable<Status> = tagStream(tag, streamingMethods::federatedHashtag)

    // TODO user streaming
}