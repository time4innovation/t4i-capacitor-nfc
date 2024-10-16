import Foundation
import Capacitor
import CoreNFC

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(ZiliconNfcPlugin)
public class ZiliconNfcPlugin: CAPPlugin, NFCNDEFReaderSessionDelegate {
    
    // MARK: - Properties

    let reuseIdentifier = "reuseIdentifier"
    var detectedMessages = [NFCNDEFMessage]()
    var session: NFCNDEFReaderSession?
    
    public func readerSession(_ session: NFCNDEFReaderSession, didDetectNDEFs messages: [NFCNDEFMessage]) {
        DispatchQueue.main.async {
            // Process detected NFCNDEFMessage objects.
            self.detectedMessages.append(contentsOf: messages)
        }
    }
    
    public func readerSession(_ session: NFCNDEFReaderSession, didDetect tags: [NFCNDEFTag]) {
        if tags.count > 1 {
            // Restart polling in 500ms
            let retryInterval = DispatchTimeInterval.milliseconds(500)
            session.alertMessage = "More than 1 tag is detected, please remove all tags and try again."
            DispatchQueue.global().asyncAfter(deadline: .now() + retryInterval, execute: {
                session.restartPolling()
            })
            return
        }
        
        // Connect to the found tag and perform NDEF message reading
        let tag = tags.first!
        session.connect(to: tag, completionHandler: { (error: Error?) in
            if nil != error {
                session.alertMessage = "Unable to connect to tag."
                session.invalidate()
                return
            }
            
            tag.readNDEF(completionHandler: { (message: NFCNDEFMessage?, error: Error?) in
                var statusMessage: String
                if nil != error || nil == message {
                    statusMessage = "Fail to read NDEF from tag"
                } else {
                    statusMessage = "Found 1 NDEF message"
                    DispatchQueue.main.async {
                        // Process detected NFCNDEFMessage objects.
                        self.detectedMessages.append(message!)
                    }
                    
                    // Extract the text payload from the message
                    var textPayload = ""
                    for record in message!.records {
                        if record.typeNameFormat == .nfcWellKnown && String(data: record.type, encoding: .utf8) == "T" { // Checking for Text record
                            let payloadData = record.payload
                            if payloadData.count > 0 {
                                let statusByte = payloadData[0]
                                let languageCodeLength = Int(statusByte & 0x3F) // Lower 6 bits of the status byte
                                if payloadData.count > languageCodeLength + 1 {
                                    let textData = payloadData.dropFirst(languageCodeLength + 1) // Skip status byte and language code
                                    if let textString = String(data: textData, encoding: .utf8) {
                                        textPayload += textString
                                    }
                                }
                            }
                        }
                    }

                    // Update the status message with the text payload
                    if !textPayload.isEmpty {
                        statusMessage = "Message: \(textPayload)"
                    }
                }
                
                session.alertMessage = statusMessage
                session.invalidate()
            })
        })
    }
    
    /// - Tag: sessionBecomeActive
    public func readerSessionDidBecomeActive(_ session: NFCNDEFReaderSession) {
        
    }
    
    /// - Tag: endScanning
    public func readerSession(_ session: NFCNDEFReaderSession, didInvalidateWithError error: Error) {
        // Check the invalidation reason from the returned error.
        if let readerError = error as? NFCReaderError {
            // Show an alert when the invalidation reason is not because of a
            // successful read during a single-tag read session, or because the
            // user canceled a multiple-tag read session from the UI or
            // programmatically using the invalidate method call.
            if (readerError.code != .readerSessionInvalidationErrorFirstNDEFTagRead)
                && (readerError.code != .readerSessionInvalidationErrorUserCanceled) {
                _ = UIAlertController(
                    title: "Session Invalidated",
                    message: error.localizedDescription,
                    preferredStyle: .alert
                )
                /*
                alertController.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                DispatchQueue.main.async {
                    self.present(alertController, animated: true, completion: nil)
                }
                */
            }
        }
        // To read new tags, a new session instance is required.
        self.session = nil
    }
    
    
    private func notifyDetectedMessages(messages: [NFCNDEFMessage]) {
        let convertedMessages = messages.map { message in
            ["records": message.records.map { record in
                ["type": record.typeNameFormat.rawValue, "identifier": record.identifier, "payload": record.payload] as [String : Any]
            }]
        }
        self.notifyListeners("nfcDetected", data: ["messages": convertedMessages])
    }

    
    private let implementation = ZiliconNfc()

    @objc func startNfcSession(_ call: CAPPluginCall) {
        guard NFCNDEFReaderSession.readingAvailable else {
            call.reject("This device doesn't support tag scanning.")
            return
        }
    
        session = NFCNDEFReaderSession(delegate: self, queue: nil, invalidateAfterFirstRead: false)
        session?.alertMessage = "Hebe deine Karte an das Gerät!"
        session?.begin()
    
        _ = call.getString("value") ?? ""
        call.resolve()
    }
}
