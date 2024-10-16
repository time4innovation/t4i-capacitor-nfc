import Foundation

@objc public class ZiliconNfc: NSObject {
    @objc public func echoDuringNfcSession(_ value: String) -> String {
        print(value)
        return value
    }
}