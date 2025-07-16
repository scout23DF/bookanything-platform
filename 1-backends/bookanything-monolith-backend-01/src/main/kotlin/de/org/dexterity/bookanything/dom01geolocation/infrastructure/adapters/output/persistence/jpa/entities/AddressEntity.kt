package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities

import de.org.dexterity.bookanything.dom01geolocation.domain.models.StatusType
import jakarta.persistence.*
import org.locationtech.jts.geom.Point
import java.util.*

@Entity
@Table(name = "tb_address")
open class AddressEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,

    @Column("ds_street_name")
    open var streetName: String,

    @Column("ds_house_number")
    open var houseNumber: String?,

    @Column("ds_floor_number")
    open var floorNumber: String?,

    @Column("ds_door_number")
    open var doorNumber: String?,

    @Column("ds_address_line2")
    open var addressLine2: String?,

    @Column("ds_postal_code")
    open var postalCode: String,

    @Column("ds_district_name")
    open var districtName: String,

    @Column("ds_city_name")
    open var cityName: String,

    @Column("ds_province_name")
    open var provinceName: String,

    @Column("ds_country_name")
    open var countryName: String,

    @Column("ge_coordinates", columnDefinition="geometry(Point,4326)" )
    open var coordinates: Point?,

    @Column("cd_status")
    @Enumerated(EnumType.STRING)
    open var status: StatusType? = StatusType.ACTIVE,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    open var district: DistrictEntity
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AddressEntity
        return id != null && id == other.id
    }

    override fun hashCode(): Int = Objects.hash(id)
}