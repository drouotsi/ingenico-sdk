import Flutter
import IngenicoConnectKit
import UIKit

public class SwiftIngenicoSdk: NSObject, FlutterPlugin, FLTApi {
    public func _passThroughA(_ a: FLTPaymentProductField, b: FLTBasicPaymentProduct, c: FLTAbstractValidationRule, d: FLTValueMap, e: FLTPaymentProductFieldDisplayElement, error: AutoreleasingUnsafeMutablePointer<FlutterError?>) {}

    private var sessionsMap = [String: Session]()
    private var paymentProductMap = [String: PaymentProduct]()

    public static func register(with registrar: FlutterPluginRegistrar) {
        let messenger: FlutterBinaryMessenger = registrar.messenger()
        let api: FLTApi & NSObjectProtocol = SwiftIngenicoSdk()
        FLTApiSetup(messenger, api)
    }

    public func createClientSessionRequest(_ input: FLTSessionRequest, error: AutoreleasingUnsafeMutablePointer<FlutterError?>) -> FLTSessionResponse? {
        let session = Session(clientSessionId: input.clientSessionId, customerId: input.customerId,
                              baseURL: input.clientApiUrl, assetBaseURL: input.assetBaseUrl, appIdentifier: input.applicationIdentifier)

        sessionsMap[session.clientSessionId] = session
        let response = FLTSessionResponse.make(withSessionId:session.clientSessionId)
        return response
    }

    private func mapDisplayHints(displayHints: PaymentItemDisplayHints) -> FLTDisplayHintsPaymentItem {
        let mapped = FLTDisplayHintsPaymentItem.make(withDisplayOrder: NSNumber(value: displayHints.displayOrder!), label: "", logoUrl: displayHints.logoPath)
        return mapped
    }

    private func mapBasicPaymentItem(basicPaymentProduct: BasicPaymentItem) -> FLTBasicPaymentProduct {
        let mapped = FLTBasicPaymentProduct.make(withId: basicPaymentProduct.identifier,
        paymentMethod: nil,
        paymentProductGroup: nil,
        minAmount: nil,
        maxAmount: nil,
        allowsRecurring: nil,
        allowsTokenization: nil,
        usesRedirectionTo3rdParty: nil,
        displayHints:mapDisplayHints(displayHints: basicPaymentProduct.displayHints))
        return mapped
    }

    private func mapType(type: FieldType) -> FLTType {
        switch type {
        case .string:
            return FLTType.string
        case .integer:
            return FLTType.string

        case .expirationDate:
            return FLTType.expirydate

        case .numericString:
            return FLTType.numericstring

        case .boolString:
            return FLTType.booleanEnum

        case .dateString:
            return FLTType.date
        }
    }

    private func mapPaymentProductField(paymentProductField: PaymentProductField) -> FLTPaymentProductField {
        let mapped = FLTPaymentProductField.make(withId: paymentProductField.identifier,
         type:mapType(type: paymentProductField.type),
         displayHints:nil,
         dataRestrictions:nil)
        return mapped
    }

    public func getBasicPaymentItemsRequest(_ input: FLTPaymentContextRequest?, completion: @escaping (FLTPaymentContextResponse?, FlutterError?) -> Void) {
        let amountOfMoney = PaymentAmountOfMoney(totalAmount: Int(truncating: input!.amountValue), currencyCode: CurrencyCode(rawValue: input!.currencyCode)!)
        let context = PaymentContext(amountOfMoney: amountOfMoney, isRecurring: input!.isRecurring as! Bool,
                                     countryCode: CountryCode(rawValue: input!.countryCode)!)

        let session = sessionsMap[input!.sessionId]!
        // TODO: add a parameter to handle the groupPaymentProducts parameter
        session.paymentItems(for: context, groupPaymentProducts: false,
                             success: { paymentItems in
                                 let response = FLTPaymentContextResponse.make(withBasicPaymentProduct: paymentItems.allPaymentItems.map(self.mapBasicPaymentItem))
                                 completion(response, nil)
                             }, failure: { error in
                                 let error = FlutterError(code: "ERROR", message: error.localizedDescription, details: nil)
                                 completion(nil, error)
                             })
    }

    public func getPaymentProductRequest(_ input: FLTGetPaymentProductRequest?, completion: @escaping (FLTPaymentProduct?, FlutterError?) -> Void) {
        let paymentProductId = input!.paymentProductId
        let amountOfMoney = PaymentAmountOfMoney(totalAmount: Int(truncating: input!.amountValue), currencyCode: CurrencyCode(rawValue: input!.currencyCode)!)
        let context = PaymentContext(amountOfMoney: amountOfMoney, isRecurring: input!.isRecurring as! Bool,
                                     countryCode: CountryCode(rawValue: input!.countryCode)!)

        let session = sessionsMap[input!.sessionId]!

        session.paymentProduct(withId: paymentProductId, context: context,
                               success: { paymentProduct in
                                   let response = FLTPaymentProduct.make(
                                    withId:paymentProduct.identifier,
                                    paymentMethod: nil,
                                    paymentProductGroup: nil,
                                    minAmount: nil,
                                    maxAmount: nil,
                                    allowsRecurring: NSNumber(value: paymentProduct.allowsRecurring),
                                    allowsTokenization: NSNumber(value: paymentProduct.allowsTokenization),
                                    usesRedirectionTo3rdParty: nil,
                                    displayHints: nil,
                                    fields: paymentProduct.fields.paymentProductFields.map(self.mapPaymentProductField))

                                   self.paymentProductMap[paymentProduct.identifier] = paymentProduct
                                   completion(response, nil);
                               }, failure: { error in
                                   let error = FlutterError(code: "ERROR", message: error.localizedDescription, details: nil)
                                   completion(nil, error)
                               })
    }

    public func preparePaymentRequest(_ input: FLTPaymentRequest?, completion: @escaping (FLTPreparedPaymentRequest?, FlutterError?) -> Void) {
        let paymentProduct = paymentProductMap[input!.paymentProductId]!

        let paymentRequest = PaymentRequest(paymentProduct: paymentProduct, accountOnFile: nil, tokenize: input!.tokenize as? Bool)

        input!.values.forEach { (key: AnyHashable, value: Any) in
            paymentRequest.setValue(forField: key as! String, value: value as! String)
        }
        let session = sessionsMap[input!.sessionId]!
        session.prepare(paymentRequest, success: { preparedPaymentRequest in
            let response = FLTPreparedPaymentRequest.make(
                withEncryptedFields: preparedPaymentRequest.encryptedFields,
                encodedClientMetaInfo:preparedPaymentRequest.encodedClientMetaInfo
                )
            completion(response, nil)
        }, failure: { error in
            let error = FlutterError(code: "ERROR", message: error.localizedDescription, details: nil)
            completion(nil, error)
        })
    }
}
