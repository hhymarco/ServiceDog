package entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 更新配置类
 * @author zbl
 *
 */
public class Config {
	private String version;//新版本号
	private String delay;//启动延迟，避免服务端过高的并发
	private List<Modify> modifies = new ArrayList<Modify>();//修改列表
	private String clientUpdateInfo;//客户端更新信息

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<Modify> getModifies() {
		return modifies;
	}

	public void setModifies(List<Modify> modifies) {
		this.modifies = modifies;
	}

	public String getClientUpdateInfo() {
		return clientUpdateInfo;
	}

	public void setClientUpdateInfo(String clientUpdateInfo) {
		this.clientUpdateInfo = clientUpdateInfo;
	}

	public String getDelay() {
		return delay;
	}

	public void setDelay(String delay) {
		this.delay = delay;
	}

}
