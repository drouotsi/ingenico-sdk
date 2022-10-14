import 'dart:async';

import 'package:ingenico_platform_interface/main.dart'
    hide PaymentProduct, BasicPaymentProduct;
import 'package:ingenico_sdk/pigeon.dart';

/// Class to interact with the Ingenico SDK
class IngenicoSdk implements IngenicoPlatform {
  static Api? _apiInstance;

  static Api get _api {
    if (_apiInstance == null) {
      _apiInstance = Api();
    }
    return _apiInstance!;
  }

  /// Convenience method for creating Session given the clientSessionId, customerId and region
  ///
  /// This is the entry point to the [IngenicoSDK] and should be used first.
  /// See documentation [on the Ingenico website](https://epayments.developer-ingenico.com/documentation/sdk/mobile/android/#GcSession)
  static Future<Session> initClientSession({
    required String clientSessionId,
    required String customerId,
    required String clientApiUrl,
    required String assetBaseUrl,
    required bool environmentIsProduction,
    required String applicationIdentifier,
  }) async {
    final sessionRequest = SessionRequest(
        clientSessionId: clientSessionId,
        customerId: customerId,
        clientApiUrl: clientApiUrl,
        assetBaseUrl: assetBaseUrl,
        environmentIsProduction: environmentIsProduction,
        applicationIdentifier: applicationIdentifier);

    final session = await _api.createClientSession(sessionRequest);

    return Session(session.sessionId);
  }
}

/// Session contains all methods needed for making a payment
///
/// This should be generated with the [IngenicoSDK.initClientSession].
/// Cannot be reused between different sessions of the app (is destroyed between each plugin restart).
/// See documentation [on the Ingenico website](https://epayments.developer-ingenico.com/documentation/sdk/mobile/android/#GcSession)
class Session {
  /// Id of the session
  final String sessionId;
  static Api? _apiInstance;

  /// Create an instance of [Session]
  Session(this.sessionId);

  static Api get _api {
    if (_apiInstance == null) {
      _apiInstance = Api();
    }
    return _apiInstance!;
  }

  /// Gets all basicPaymentItems for a given payment context
  ///
  /// Can be used to see which payment product is available (Visa, Mastercard, Paypal ...)
  /// See documentation [on the Ingenico website](https://epayments.developer-ingenico.com/documentation/sdk/mobile/android/#BasicPaymentItems)
  Future<List<BasicPaymentProduct>> getBasicPaymentProducts({
    required double amountValue,
    required String currencyCode,
    required String countryCode,
    required bool isRecurring,
  }) async {
    final paymentContextRequest = PaymentContextRequest(
        amountValue: amountValue,
        currencyCode: currencyCode,
        countryCode: countryCode,
        isRecurring: isRecurring,
        sessionId: sessionId);

    final response = await _api.getBasicPaymentItems(paymentContextRequest);

    return response.basicPaymentProduct.cast<BasicPaymentProduct>();
  }

  /// Once the [PaymentProduct] have been selected by the user
  /// you can use this function to gather all the validation informatinons about the product
  ///
  /// See documentation [on the Ingenico website](https://epayments.developer-ingenico.com/documentation/sdk/mobile/android/#PaymentProduct)
  Future<PaymentProduct> getPaymentProduct({
    required String paymentProductId,
    required double amountValue,
    required String currencyCode,
    required String countryCode,
    required bool isRecurring,
  }) async {
    final paymentProductRequest = GetPaymentProductRequest(
        paymentProductId: paymentProductId,
        amountValue: amountValue,
        currencyCode: currencyCode,
        countryCode: countryCode,
        isRecurring: isRecurring,
        sessionId: sessionId);

    final response = await _api.getPaymentProduct(paymentProductRequest);

    return response;
  }

  /// Get the encrypted payment data to be send to the backend in order to process the payment
  /// Final step of the mobile SDK payment
  ///
  /// See documentation [on the Ingenico website](https://epayments.developer-ingenico.com/documentation/sdk/mobile/android/#PreparedPaymentRequest)
  Future<PreparedPaymentRequest> preparePaymentRequest({
    required String paymentProductId,
    required Map<String, String> values,
    required String currencyCode,
    required bool tokenize,
  }) {
    final request = PaymentRequest(
        paymentProductId: paymentProductId,
        values: values,
        tokenize: tokenize,
        sessionId: sessionId);

    return _api.preparePaymentRequest(request);
  }
}
