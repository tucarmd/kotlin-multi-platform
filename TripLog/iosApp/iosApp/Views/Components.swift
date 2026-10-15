import Charts
import MapKit
import SwiftUI
import Shared

/// [App 4 · B6] A real map with the route drawn on it. The Android twin draws the same route on a Canvas.
///
/// One MapPolyline per recording segment, so a pause shows as a gap instead of a straight line across it.
struct RouteMap: View {
    let points: [TrackPoint]
    var followUser = false
    @State private var position: MapCameraPosition = .automatic

    var body: some View {
        Map(position: $position) {
            if followUser { UserAnnotation() }
            ForEach(segments.indices, id: \.self) { index in
                MapPolyline(coordinates: segments[index]).stroke(.teal, lineWidth: 5)
            }
            if let first = points.first {
                Marker("", coordinate: first.coordinate).tint(.green)
            }
            if let last = points.last, points.count > 1 {
                Marker("", coordinate: last.coordinate).tint(.red)
            }
        }
        .onAppear { if followUser { position = .userLocation(fallback: .automatic) } }
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var segments: [[CLLocationCoordinate2D]] {
        var result: [[CLLocationCoordinate2D]] = []
        var lastSegment: Int32 = -1
        for point in points {
            if point.segment != lastSegment {
                result.append([])
                lastSegment = point.segment
            }
            result[result.count - 1].append(point.coordinate)
        }
        return result
    }
}

extension TrackPoint {
    var coordinate: CLLocationCoordinate2D { CLLocationCoordinate2D(latitude: latitude, longitude: longitude) }
}

/// [App 4 · B6] One labelled number, e.g. "Distance / 1.23 km". Twin of StatTile in Components.kt.
struct StatTile: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            Text(value).font(.title3.weight(.semibold)).monospacedDigit()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.quaternary.opacity(0.5), in: RoundedRectangle(cornerRadius: 12))
    }
}

/// [App 4 · B6] Speed over the trip in km/h, with Swift Charts. Twin of SpeedChart in Components.kt.
struct SpeedChart: View {
    let speeds: [Double] // m/s

    var body: some View {
        Chart(Array(speeds.enumerated()), id: \.offset) { item in
            LineMark(x: .value("Sample", item.offset), y: .value("km/h", item.element * 3.6))
                .interpolationMethod(.catmullRom)
        }
        .chartXAxis(.hidden)
        .frame(height: 120)
    }
}
