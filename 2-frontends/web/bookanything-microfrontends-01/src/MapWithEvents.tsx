import { useState } from 'react';
import { Marker, Popup, useMapEvents } from 'react-leaflet';

const MapWithEvents = () => {
    const [position, setPosition] = useState([1.3521, 103.8198]);
    const map = useMapEvents({
        click(e) {
            setPosition(e.latlng);
            map.flyTo(e.latlng, map.getZoom());
        },
    });

    return position === null ? null : (
        <Marker position={position}>
            <Popup>You are here</Popup>
        </Marker>
    );
};

export default MapWithEvents;