package com.ingenico.flutter_sdk
import android.content.Context
import androidx.annotation.NonNull
import com.onlinepayments.sdk.client.android.asynctask.BasicPaymentItemsAsyncTask.BasicPaymentItemsCallListener
import com.onlinepayments.sdk.client.android.asynctask.PaymentProductAsyncTask
import com.onlinepayments.sdk.client.android.communicate.C2sCommunicatorConfiguration
import com.onlinepayments.sdk.client.android.model.*
import com.onlinepayments.sdk.client.android.model.api.ErrorResponse
import com.onlinepayments.sdk.client.android.model.paymentproduct.*
import com.onlinepayments.sdk.client.android.model.paymentproduct.displayhints.DisplayHintsPaymentItem
import com.onlinepayments.sdk.client.android.model.validation.ValidationRule
import com.onlinepayments.sdk.client.android.session.Session
import com.onlinepayments.sdk.client.android.session.SessionEncryptionHelper.OnPaymentRequestPreparedListener
import io.flutter.embedding.engine.plugins.FlutterPlugin
/** IngenicoSdkPlugin */
class IngenicoSdkPlugin : FlutterPlugin, Messages.Api {
     val sessionsMap: HashMap<String, Session> = HashMap()
     val paymentProductMap: HashMap<String, PaymentProduct> =
        HashMap()
     var context: Context? = null
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Messages.Api.setup(flutterPluginBinding.binaryMessenger, this)
        context = flutterPluginBinding.applicationContext
    }
    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Messages.Api.setup(binding.binaryMessenger, null)
    }
    override fun createClientSession(arg: Messages.SessionRequest): Messages.SessionResponse {
        val session: Session = C2sCommunicatorConfiguration.initWithClientSessionId(
            arg.clientSessionId,
            arg.customerId,
            arg.clientApiUrl,
            arg.assetBaseUrl,
            arg.environmentIsProduction,
            arg.applicationIdentifier
        )
        val messageSession = Messages.SessionResponse.Builder()
        messageSession.setSessionId(session.clientSessionId)
        val builtSession = messageSession.build()
        sessionsMap[builtSession.getSessionId()] = session
        return builtSession
    }

    private fun mapDisplayHints(displayHints: DisplayHintsPaymentItem): Messages.DisplayHintsPaymentItem {
        var mapped = Messages.DisplayHintsPaymentItem.Builder()
        mapped.setDisplayOrder(displayHints.displayOrder.toLong())
        mapped.setLabel(displayHints.label)
        mapped.setLogoUrl(displayHints.logoUrl)
        return mapped.build()
    }
    private fun mapBasicPaymentProduct(basicPaymentProduct: BasicPaymentProduct): Messages.BasicPaymentProduct {
        var mapped = Messages.BasicPaymentProduct.Builder()
        mapped.setAllowsRecurring(basicPaymentProduct.allowsRecurring())
        mapped.setAllowsTokenization(basicPaymentProduct.allowsTokenization())
        mapped.setDisplayHints(mapDisplayHints(basicPaymentProduct.displayHints))
        mapped.setId(basicPaymentProduct.id)
        mapped.setMaxAmount(basicPaymentProduct.maxAmount.toDouble())
        mapped.setMinAmount(basicPaymentProduct.minAmount.toDouble())
        mapped.setPaymentMethod(basicPaymentProduct.paymentMethod)
        mapped.setPaymentProductGroup(basicPaymentProduct.paymentProductGroup)
        mapped.setUsesRedirectionTo3rdParty(basicPaymentProduct.usesRedirectionTo3rdParty())
        return mapped.build();
    }
    private fun mapBasicPaymentItem(basicPaymentProduct: BasicPaymentItem): Messages.BasicPaymentProduct {
        var mapped = Messages.BasicPaymentProduct.Builder()
        mapped.setDisplayHints(mapDisplayHints(basicPaymentProduct.displayHints))
        mapped.setId(basicPaymentProduct.id)
        return mapped.build();
    }
    private fun mapType(basicPaymentProduct: PaymentProductField.Type): Messages.Type {
        return when (basicPaymentProduct) {
            PaymentProductField.Type.STRING -> Messages.Type.string
            PaymentProductField.Type.INTEGER -> Messages.Type.integer
            PaymentProductField.Type.NUMERICSTRING -> Messages.Type.numericstring
            PaymentProductField.Type.EXPIRYDATE -> Messages.Type.expirydate
            PaymentProductField.Type.BOOLEAN -> Messages.Type.booleanEnum
            PaymentProductField.Type.DATE -> Messages.Type.date
        }
    }
    private fun mapPaymentProductField(paymentProductField: PaymentProductField): Messages.PaymentProductField {
        var mapped = Messages.PaymentProductField.Builder()
        mapped.setId(paymentProductField.id)
        mapped.setType(mapType(paymentProductField.type))
        return mapped.build();
    }
    override fun getBasicPaymentItems(
        arg: Messages.PaymentContextRequest,
        result: Messages.Result<Messages.PaymentContextResponse>
    ) {
        val amountValue: Long = arg.amountValue.toLong()
        val currencyCode = CurrencyCode.valueOf(arg.currencyCode)
        val amountOfMoney = AmountOfMoney(amountValue, currencyCode)
        val countryCode = CountryCode.valueOf(arg.countryCode)
        val isRecurring = arg.isRecurring
        val paymentContext = PaymentContext(amountOfMoney, countryCode, isRecurring)
        val error = """
                           
                            amountValue -> """+ amountValue +"""
                            amountValueType -> """+ amountValue::class.qualifiedName +"""
                            currencyCode -> """+ currencyCode +"""
                            currencyCodeType -> """+ currencyCode::class.qualifiedName +"""
                           amountOfMoney.amount -> """+ amountOfMoney.amount +"""
                            amountOfMoney.amountTYPE -> """+ amountOfMoney.amount::class.qualifiedName +"""
                             amountOfMoney.currencyCode -> """+ amountOfMoney.currencyCode +"""
                              amountOfMoney.currencyCodeTYPE -> """+ amountOfMoney.currencyCode::class.qualifiedName +"""
                              amountOfMoney -> """+ amountOfMoney +"""
                               amountOfMoneyTYPE -> """+ amountOfMoney::class.qualifiedName +"""
                            countryCode -> """+ countryCode +"""
                             countryCodeTYPE -> """+ countryCode::class.qualifiedName+"""
                            isRecurring -> """+ isRecurring +"""
                             isRecurringTYPE -> """+ isRecurring::class.qualifiedName +"""
                            paymentContext -> """+ paymentContext.toString() +"""
                            init
                        """
        println(error)
        val listener: BasicPaymentItemsCallListener = object : BasicPaymentItemsCallListener {
            override fun onBasicPaymentItemsCallComplete(basicPaymentItems: BasicPaymentItems) {
                val response = Messages.PaymentContextResponse.Builder()
                if (basicPaymentItems is BasicPaymentProducts) {
                    val basicPaymentProducts = basicPaymentItems as BasicPaymentProducts
                    response.setBasicPaymentProduct(basicPaymentProducts.basicPaymentProducts.map{
                        mapBasicPaymentProduct(it)
                    })
                    result.success(response.build())
                    return
                }
                response.setBasicPaymentProduct(basicPaymentItems.basicPaymentItems.map{
                    mapBasicPaymentItem(it)
                })
                result.success(response.build())
            }
            override fun onBasicPaymentItemsCallError(error: ErrorResponse) {
                val error = """
                           
                            amountValue -> """+ amountValue +"""
                            amountValueType -> """+ amountValue::class.qualifiedName +"""
                            currencyCode -> """+ currencyCode +"""
                            currencyCodeType -> """+ currencyCode::class.qualifiedName +"""
                           amountOfMoney.amount -> """+ amountOfMoney.amount +"""
                            amountOfMoney.amountTYPE -> """+ amountOfMoney.amount::class.qualifiedName +"""
                             amountOfMoney.currencyCode -> """+ amountOfMoney.currencyCode +"""
                              amountOfMoney.currencyCodeTYPE -> """+ amountOfMoney.currencyCode::class.qualifiedName +"""
                              amountOfMoney -> """+ amountOfMoney +"""
                               amountOfMoneyTYPE -> """+ amountOfMoney::class.qualifiedName +"""
                            countryCode -> """+ countryCode +"""
                             countryCodeTYPE -> """+ countryCode::class.qualifiedName+"""
                            isRecurring -> """+ isRecurring +"""
                             isRecurringTYPE -> """+ isRecurring::class.qualifiedName +"""
                            apiError -> """+ error.apiError +"""
                            errorMessage -> """+ error.message.toString() +"""
                            EOF
                        """
                result.error(Error(error))
            }
        }
        val session = sessionsMap[arg.sessionId] ?: result.error(Error("Cannot find session"))
        if (session is Session) {
            session.getBasicPaymentItems(context, paymentContext, listener, false)
        }
    }
    override fun getPaymentProduct(
        arg: Messages.GetPaymentProductRequest,
        result: Messages.Result<Messages.PaymentProduct>
    ) {
        val listener: PaymentProductAsyncTask.PaymentProductCallListener = object :
            PaymentProductAsyncTask.PaymentProductCallListener {
            override fun onPaymentProductCallComplete(paymentProduct: PaymentProduct) {
                val response = Messages.PaymentProduct.Builder()
                paymentProductMap[paymentProduct.id] = paymentProduct
                response.setFields(paymentProduct.paymentProductFields.map { mapPaymentProductField(it) })
                response.setId(paymentProduct.id)
                response.setAllowsRecurring(paymentProduct.allowsRecurring())
                response.setAllowsTokenization(paymentProduct.allowsTokenization())
                response.setMaxAmount(paymentProduct.maxAmount?.toDouble())
                response.setMinAmount(paymentProduct.minAmount?.toDouble())
                response.setDisplayHints(mapDisplayHints(paymentProduct.displayHints))
                result.success(response.build())
            }
            override fun onPaymentProductCallError(error: ErrorResponse) {
                result.error(Error(error.message))
            }
        }
        val amountValue: Long = arg.amountValue.toLong()
        val currencyCode = CurrencyCode.valueOf(arg.currencyCode)
        val amountOfMoney = AmountOfMoney(amountValue, currencyCode)
        val countryCode = CountryCode.valueOf(arg.countryCode)
        val isRecurring = arg.isRecurring
        val paymentContext = PaymentContext(amountOfMoney, countryCode, isRecurring)
        val session = sessionsMap[arg.sessionId] ?: throw Error("Cannot find session")
        session.getPaymentProduct(context, arg.paymentProductId, paymentContext, listener)
    }
    override fun preparePaymentRequest(
        arg: Messages.PaymentRequest,
        result: Messages.Result<Messages.PreparedPaymentRequest>
    ) {
        val paymentRequest = PaymentRequest()
        paymentRequest.paymentProduct = paymentProductMap[arg.paymentProductId]
        paymentRequest.tokenize = arg.tokenize
        arg.values.entries.forEach { e -> paymentRequest.setValue(e.key as String, e.value as String) }
        val listener =
            OnPaymentRequestPreparedListener { preparedPaymentRequest ->
                if (preparedPaymentRequest == null ||
                    preparedPaymentRequest.encryptedFields == null
                ) {
                    result.error(Error("Couldn't prepare the payment"))
                } else {
                    val response = Messages.PreparedPaymentRequest.Builder()
                    response.setEncryptedFields(preparedPaymentRequest.encryptedFields)
                    response.setEncodedClientMetaInfo(preparedPaymentRequest.encodedClientMetaInfo)
                    result.success(response.build())
                }
            }
        val session = sessionsMap[arg.sessionId] ?: throw Error("Cannot find session")
        session.preparePaymentRequest(paymentRequest, context, listener)
    }
}