package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import de.org.dexterity.bookanything.dom01geolocation.domain.models.StatusType
import jakarta.persistence.*
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point


@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tp_geo_location", discriminatorType = DiscriminatorType.STRING)
@Table(name = "tb_geo_location")
abstract class AbstractBaseGeoLocationEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column("ds_name")
    val name: String,

    @Column("tp_geo_location", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    val type: GeoLocationType,

    @Column(name = "ge_geographic_boundary",  columnDefinition = "geometry")
    val boundaryRepresentation: Geometry?,

    @Column("parent_id", insertable = false, updatable = false)
    val parentId: Long? = null,

)

@Entity
@DiscriminatorValue("CONTINENT")
data class ContinentEntity (
    override val name: String,
    override val boundaryRepresentation: Geometry?,

    @OneToMany(mappedBy = "continent", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val regionsList: List<RegionEntity>? = emptyList()

) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.CONTINENT,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("REGION")
data class RegionEntity (
    override val name: String,
    override val boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "continent_id")
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_region_continent"))
    val continent: ContinentEntity,

    @OneToMany(mappedBy = "region", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val countriesList: List<CountryEntity>? = emptyList()

) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.REGION,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("COUNTRY")
data class CountryEntity (
    override val name: String,
    override val boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "region_id")
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_country_region"))
    val region: RegionEntity,

    @OneToMany(mappedBy = "country", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val provincesList: List<ProvinceEntity>? = emptyList()

) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.COUNTRY,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("PROVINCE")
data class ProvinceEntity (
    override val name: String,
    override val boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "country_id")
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_province_country"))
    val country: CountryEntity,

    @OneToMany(mappedBy = "province", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val citiesList: List<CityEntity>? = emptyList()

) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.PROVINCE,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("CITY")
data class CityEntity (
    override val name: String,
    override val boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "province_id")
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_city_province"))
    val province: ProvinceEntity,

    @OneToMany(mappedBy = "city", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val districtsList: List<DistrictEntity>? = emptyList()

) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.CITY,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("DISTRICT")
data class DistrictEntity (
    override val name: String,
    override val boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "city_id")
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_district_city"))
    val city: CityEntity,

    @OneToMany(mappedBy = "district", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val addressesList: List<AddressEntity>? = emptyList()

) : AbstractBaseGeoLocationEntity(
    name = name,
    type = GeoLocationType.DISTRICT,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@Table(name = "tb_address")
data class AddressEntity (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column("ds_street_name")
    val streetName: String,

    @Column("ds_house_number")
    val houseNumber: String?,

    @Column("ds_floor_number")
    val floorNumber: String?,

    @Column("ds_door_number")
    val doorNumber: String?,

    @Column("ds_address_line2")
    val addressLine2: String?,

    @Column("ds_postal_code")
    val postalCode: String,

    @Column("ds_district_name")
    val districtName: String,

    @Column("ds_city_name")
    val cityName: String,

    @Column("ds_province_name")
    val provinceName: String,

    @Column("ds_country_name")
    val countryName: String,

    @Column("ge_coordinates", columnDefinition = "point")
    val coordinates: Point?,

    @Column("cd_status")
    @Enumerated(EnumType.STRING)
    val status: StatusType? = StatusType.ACTIVE,


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    val district: DistrictEntity

)
