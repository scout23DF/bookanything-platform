import { useEffect } from 'react';
import { useMap } from 'react-leaflet';

import SingaporesParks from './geojson/ParkFacilities-partial.json';
import ParkIcon from './assets/park-icon.png';

const Parks = () => {
    const map = useMap();

    useEffect(() => {
        console.log('map', map);
        if (!map) {
            return;
        }

        const parkIcon = L.icon({
            iconUrl: ParkIcon, // URL to your custom icon
            iconSize: [24, 24], // Size of the icon [width, height]
            iconAnchor: [16, 32], // Point of the icon that will correspond to marker's location
            popupAnchor: [0, -32], // Point from which the popup should open relative to the iconAnchor
        });

        /*
        const parksInSingaporeGeoJson = new L.GeoJSON(SingaporesParks);
        */

        const parksInSingaporeGeoJson = new L.GeoJSON(SingaporesParks, {
            pointToLayer: (feature = {}, latlng) => {
                return L.marker(latlng, {
                    icon: parkIcon,
                });
            },
            onEachFeature: (feature = {}, layer) => {
                const { properties = {} } = feature;
                const { Name } = properties;
                if (!Name) {
                    return;
                }
                layer.bindPopup(`<p>${Name}</p>`);
            },
        });

        parksInSingaporeGeoJson.addTo(map);

    }, [map]);
    return <></>;
};
export default Parks;