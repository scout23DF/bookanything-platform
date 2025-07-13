package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.StatusType
import jakarta.persistence.*
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import java.util.Objects

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tp_geo_location", discriminatorType = DiscriminatorType.STRING)
@Table(name = "tb_geo_location")
abstract class AbstractBaseGeoLocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column("ds_name")
    val name: String,

    @Column("tp_geo_location", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    val type: GeoLocationType,

    @Column(name = "ge_geographic_boundary", columnDefinition = "geometry")
    val boundaryRepresentation: Geometry?,

    @Column("parent_id", insertable = false, updatable = false)
    val parentId: Long? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AbstractBaseGeoLocationEntity
        return id != null && id == other.id
    }

    override fun hashCode(): Int = Objects.hash(id)

    override fun toString(): String {
        return "${this.javaClass.simpleName}(id=$id, name='$name', type=$type)"
    }
}

@Entity
@DiscriminatorValue("CONTINENT")
open class ContinentEntity(
    name: String,
    boundaryRepresentation: Geometry?,

    @OneToMany(mappedBy = "continent", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY)
    open val regionsList: List<RegionEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.CONTINENT,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("REGION")
open class RegionEntity(
    name: String,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_region_continent"))
    open val continent: ContinentEntity,

    @OneToMany(mappedBy = "region", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY)
    open val countriesList: List<CountryEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.REGION,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("COUNTRY")
open class CountryEntity(
    name: String,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_country_region"))
    open val region: RegionEntity,

    @OneToMany(mappedBy = "country", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY)
    open val provincesList: List<ProvinceEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.COUNTRY,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("PROVINCE")
open class ProvinceEntity(
    name: String,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_province_country"))
    open val country: CountryEntity,

    @OneToMany(mappedBy = "province", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY)
    open val citiesList: List<CityEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.PROVINCE,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("CITY")
open class CityEntity(
    name: String,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_city_province"))
    open val province: ProvinceEntity,

    @OneToMany(mappedBy = "city", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY)
    open val districtsList: List<DistrictEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.CITY,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("DISTRICT")
open class DistrictEntity(
    name: String,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_district_city"))
    open val city: CityEntity,

    @OneToMany(mappedBy = "district", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY)
    open val addressesList: List<AddressEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.DISTRICT,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@Table(name = "tb_address")
open class AddressEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long? = null,

    @Column("ds_street_name")
    open val streetName: String,

    @Column("ds_house_number")
    open val houseNumber: String?,

    @Column("ds_floor_number")
    open val floorNumber: String?,

    @Column("ds_door_number")
    open val doorNumber: String?,

    @Column("ds_address_line2")
    open val addressLine2: String?,

    @Column("ds_postal_code")
    open val postalCode: String,

    @Column("ds_district_name")
    open val districtName: String,

    @Column("ds_city_name")
    open val cityName: String,

    @Column("ds_province_name")
    open val provinceName: String,

    @Column("ds_country_name")
    open val countryName: String,

    @Column("ge_coordinates", columnDefinition = "point")
    open val coordinates: Point?,

    @Column("cd_status")
    @Enumerated(EnumType.STRING)
    open val status: StatusType? = StatusType.ACTIVE,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    open val district: DistrictEntity
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AddressEntity
        return id != null && id == other.id
    }

    override fun hashCode(): Int = Objects.hash(id)
}