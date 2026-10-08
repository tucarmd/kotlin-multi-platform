import Testing
import UIKit
@testable import NativeDeviceMonitor

/// Day 10 — pure Media rules (no camera needed). Same cases as `android/.../nativecore/media/MediaInfoTest.kt`.
struct MediaInfoTests {

    @Test func formatBytesUsesBinaryUnits() {
        #expect(formatBytes(0) == "0 B")
        #expect(formatBytes(1023) == "1023 B")
        #expect(formatBytes(1536) == "1.5 KB")
        #expect(formatBytes(2 * 1024 * 1024) == "2.0 MB")
    }

    /// A fake 40×30 camera image → correct pixel info + jpeg mime + has a size.
    @Test func infoFromCameraImage() {
        let image = UIGraphicsImageRenderer(size: CGSize(width: 40, height: 30),
                                            format: { let f = UIGraphicsImageRendererFormat(); f.scale = 1; return f }())
            .image { ctx in UIColor.red.setFill(); ctx.fill(CGRect(x: 0, y: 0, width: 40, height: 30)) }
        let info = MediaManager.info(fromCamera: image)
        #expect(info.width == 40)
        #expect(info.height == 30)
        #expect(info.mimeType == "image/jpeg")
        #expect(info.source == .camera)
        #expect((info.sizeBytes ?? 0) > 0)
    }
}
