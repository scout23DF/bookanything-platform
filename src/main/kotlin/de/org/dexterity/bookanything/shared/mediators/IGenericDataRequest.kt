package de.org.dexterity.bookanything.shared.mediators

import java.util.UUID

interface IGenericDataRequest<TObjResponse> {
    val commandId: UUID
}
