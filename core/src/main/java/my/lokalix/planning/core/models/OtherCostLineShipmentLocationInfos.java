package my.lokalix.planning.core.models;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.ShipmentLocationEntity;

@Value
@Getter
@Setter
public class OtherCostLineShipmentLocationInfos {
  UUID otherCostLineId;
  ShipmentLocationEntity shipmentLocation;
  CurrencyEntity currency;
}
