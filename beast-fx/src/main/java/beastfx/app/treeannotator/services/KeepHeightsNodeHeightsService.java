package beastfx.app.treeannotator.services;

public class KeepHeightsNodeHeightsService implements NodeHeightSettingService {

	@Override
	public String getServiceName() {
		return "keep";
	}

	@Override
	public String getDescription() {
		return "Keep target heights";
	}

}
