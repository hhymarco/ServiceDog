package entity;

/**
 * 修改详细信息
 * @author zbl
 *
 */
public class Modify {
	private String name;// 文件名
	private String alias;// 文件别名（一般和文件名一样，只有当文件名存同名的情况下才会用到）
	private String path;// 相对路径
	private String operationType;// 操作类型（add,replace,delete）
	private String md5;//md5（用于验证文件完整性）

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}
}
