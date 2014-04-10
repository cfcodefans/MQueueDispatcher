package com.thenetcircle.services.dispatcher.failsafe;

import com.thenetcircle.services.dispatcher.failsafe.IFailsafe.StorageType;

public class FailsafeCfg {
	private Integer id;

	private StorageType storageType = StorageType.sql;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public StorageType getStorageType() {
		return storageType;
	}

	public void setStorageType(StorageType storageType) {
		this.storageType = storageType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"FailsafeCfg\",id:").append(id).append(", storageType:").append(storageType).append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((storageType == null) ? 0 : storageType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FailsafeCfg))
			return false;
		FailsafeCfg other = (FailsafeCfg) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (storageType != other.storageType)
			return false;
		return true;
	}
	
	
}
