package io.horizontalsystems.ethereumkit.spv.net.tasks

import io.horizontalsystems.ethereumkit.spv.core.ITask
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature

class SendTransactionTask(val sendId: Int,
                          val rawTransaction: RawTransaction,
                          val nonce: Long,
                          val signature: Signature) : ITask
