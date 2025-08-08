// MapComponent.tsx
import React, { useState } from 'react';
import {
    Circle,
    LayerGroup,
    LayersControl,
    MapContainer,
    Marker,
    Polygon,
    Popup,
    Rectangle,
    TileLayer,
} from 'react-leaflet';

import MapWithEvents from './MapWithEvents';
import Parks from './Parks';
import L from 'leaflet';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
import 'leaflet/dist/leaflet.css';

// Default marker icon
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: markerIcon2x,
    iconUrl: markerIcon,
    shadowUrl: markerShadow,
});

// Define positions for Polygon, Circle, and Rectangle
const polygonPositions = [
    [1.3521, 103.8198],
    [1.3521, 103.8197],
    [1.3521, 103.8196],
];

const circleCenter = [1.3521, 103.8198];
const rectangleBounds = [
    [1.3521, 103.8197],
    [1.3521, 103.8196],
];

const MapComponent: React.FC = () => {
    const [mapClickPosition, setMapClickPosition] = useState<string | null>(null);
    const [zoomLevel, setZoomLevel] = useState<number>(13);

    // Handler for map click
    const handleMapClick = event => {
        const { lat, lng } = event.latlng;
        console.log('handleMapClick');
        setMapClickPosition(`Latitude: ${lat}, Longitude: ${lng}`);
    };

    // Handler for zoom change
    const handleZoomEnd = event => {
        setZoomLevel(event.target.getZoom());
    };

    const { BaseLayer, Overlay } = LayersControl;

    return (
        <MapContainer
            center={[1.3521, 103.8198]}
            style={{ height: '100vh', width: '100%' }}
            zoom={zoomLevel}
            onClick={e => handleMapClick(e)} // Map click event
            onZoomEnd={handleZoomEnd} // Zoom end event
        >
            <LayersControl position='topright'>
                {/* Base Layers */}
                <BaseLayer checked name='OpenStreetMap'>
                    <LayerGroup>
                        <TileLayer
                            url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
                            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        />
                        <Marker position={[1.3521, 103.8198]}>
                            <Popup>
                                A pretty CSS3 popup. <br /> Easily customizable.
                            </Popup>
                        </Marker>

                        {/* Add Polygon */}
                        <Polygon positions={polygonPositions} color='purple' />

                        {/* Add Circle */}
                        <Circle center={circleCenter} radius={500} color='blue' />

                        {/* Add Rectangle */}
                        <Rectangle bounds={rectangleBounds} color='green' />
                        <MapWithEvents />

                        {<Parks />}
                    </LayerGroup>
                </BaseLayer>
                <BaseLayer name='Satellite'>
                    <TileLayer
                        url='https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png'
                        attribution='&copy; <a href="https://opentopomap.org/">OpenTopoMap</a> contributors'
                    />
                </BaseLayer>
                <Overlay checked name='Marker'>
                    <Marker position={[1.3521, 103.8198]} />
                </Overlay>
            </LayersControl>
        </MapContainer>
    );
};

export default MapComponent;