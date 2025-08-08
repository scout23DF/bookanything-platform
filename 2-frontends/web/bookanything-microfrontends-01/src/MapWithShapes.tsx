// src/MapWithShapes.tsx
import React from 'react';
import {
    Circle,
    MapContainer,
    Polygon,
    Rectangle,
    TileLayer,
} from 'react-leaflet';

import 'leaflet/dist/leaflet.css';


const MapWithShapes: React.FC = () => {
    // Define positions for Polygon, Circle, and Rectangle
    const polygonPositions = [
        [51.51, -0.12],
        [51.51, -0.1],
        [51.52, -0.1],
    ];

    const circleCenter = [51.505, -0.09];
    const rectangleBounds = [
        [51.49, -0.08],
        [51.5, -0.06],
    ];

    return (
        <MapContainer
            center={[51.505, -0.09]}
            zoom={13}
            style={{ height: '100vh', width: '100%' }}
        >
            <TileLayer
                url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            />

            {/* Add Polygon */}
            <Polygon positions={polygonPositions} color='purple' />

            {/* Add Circle */}
            <Circle center={circleCenter} radius={500} color='blue' />

            <Circle
                center={circleCenter}
                radius={500}
                color='red'
                fillColor='pink'
                fillOpacity={0.5}
                weight={2}
            />

            {/* Add Rectangle */}
            <Rectangle bounds={rectangleBounds} color='green' />
        </MapContainer>
    );
};

export default MapWithShapes;
