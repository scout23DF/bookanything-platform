package de.org.dexterity.bookanything.dom01geolocation.infrastructure.adapters.output.persistence.jpa.entities

import de.org.dexterity.bookanything.dom01geolocation.domain.models.GeoLocationType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Geometry
import java.util.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tp_geo_location", discriminatorType = DiscriminatorType.STRING)
@Table(
    name = "tb_geo_location",
    indexes = [Index(name = "idx_friendly_id", columnList = "ds_friendly_id")]
)
abstract class AbstractBaseGeoLocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long? = null,

    @Column("ds_friendly_id", nullable = false, length = 40)
    open var friendlyId: String,

    @Column("ds_name")
    open var name: String,

    @Column("ds_alias", nullable = true, length = 20)
    open var alias: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_additional_details", columnDefinition = "jsonb")
    open var additionalDetailsMap: Map<String, Any?>? = null,

    @Column("tp_geo_location", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    open val type: GeoLocationType,

    @Column(name = "ge_geographic_boundary", columnDefinition = "geometry")
    open var boundaryRepresentation: Geometry?,

    @Column("parent_id", insertable = false, updatable = false)
    open val parentId: Long? = null,
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
    friendlyId: String,
    name: String,
    alias: String? = null,
    additionalDetailsMap: Map<String, Any?>? = null,
    boundaryRepresentation: Geometry?,

    @OneToMany(mappedBy = "continent", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY, targetEntity = RegionEntity::class)
    open var regionsList: List<RegionEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    friendlyId = friendlyId,
    name = name,
    alias = alias,
    additionalDetailsMap = additionalDetailsMap,
    type = GeoLocationType.CONTINENT,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("REGION")
open class RegionEntity(
    friendlyId: String,
    name: String,
    alias: String? = null,
    additionalDetailsMap: Map<String, Any?>? = null,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_region_continent"))
    open var continent: ContinentEntity,

    @OneToMany(mappedBy = "region", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY, targetEntity = CountryEntity::class)
    open var countriesList: List<CountryEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    friendlyId = friendlyId,
    name = name,
    alias = alias,
    additionalDetailsMap = additionalDetailsMap,
    type = GeoLocationType.REGION,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("COUNTRY")
open class CountryEntity(
    friendlyId: String,
    name: String,
    alias: String? = null,
    additionalDetailsMap: Map<String, Any?>? = null,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_country_region"))
    open var region: RegionEntity,

    @OneToMany(mappedBy = "country", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY, targetEntity = ProvinceEntity::class)
    open var provincesList: List<ProvinceEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    friendlyId = friendlyId,
    name = name,
    alias = alias,
    additionalDetailsMap = additionalDetailsMap,
    type = GeoLocationType.COUNTRY,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("PROVINCE")
open class ProvinceEntity(
    friendlyId: String,
    name: String,
    alias: String? = null,
    additionalDetailsMap: Map<String, Any?>? = null,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_province_country"))
    open var country: CountryEntity,

    @OneToMany(mappedBy = "province", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY, targetEntity = CityEntity::class)
    open var citiesList: List<CityEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    friendlyId = friendlyId,
    name = name,
    alias = alias,
    additionalDetailsMap = additionalDetailsMap,
    type = GeoLocationType.PROVINCE,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("CITY")
open class CityEntity(
    friendlyId: String,
    name: String,
    alias: String? = null,
    additionalDetailsMap: Map<String, Any?>? = null,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_city_province"))
    open var province: ProvinceEntity,

    @OneToMany(mappedBy = "city", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY, targetEntity = DistrictEntity::class)
    open var districtsList: List<DistrictEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    friendlyId = friendlyId,
    name = name,
    alias = alias,
    additionalDetailsMap = additionalDetailsMap,
    type = GeoLocationType.CITY,
    boundaryRepresentation = boundaryRepresentation
)

@Entity
@DiscriminatorValue("DISTRICT")
open class DistrictEntity(
    friendlyId: String,
    name: String,
    alias: String? = null,
    additionalDetailsMap: Map<String, Any?>? = null,
    boundaryRepresentation: Geometry?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk01_district_city"))
    open var city: CityEntity,

    @OneToMany(mappedBy = "district", cascade = [CascadeType.REFRESH], fetch = FetchType.LAZY, targetEntity = AddressEntity::class)
    open var addressesList: List<AddressEntity>? = emptyList()
) : AbstractBaseGeoLocationEntity(
    friendlyId = friendlyId,
    name = name,
    alias = alias,
    additionalDetailsMap = additionalDetailsMap,
    type = GeoLocationType.DISTRICT,
    boundaryRepresentation = boundaryRepresentation
)
