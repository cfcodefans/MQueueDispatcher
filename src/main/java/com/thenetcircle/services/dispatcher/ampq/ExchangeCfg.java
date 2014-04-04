package com.thenetcircle.services.dispatcher.ampq;

import com.thenetcircle.services.dispatcher.cfg.Configuration;

public class ExchangeCfg extends Configuration {
	private static final long serialVersionUID = 1L;

	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private String exchangeName;
	private boolean durable = true;
	private boolean autoDelete = false;
	private String type = "direct";
	
	private ServerCfg serverCfg;
	
	public ServerCfg getServerCfg() {
		return serverCfg;
	}

	public void setServerCfg(ServerCfg serverCfg) {
		this.serverCfg = serverCfg;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}

	public boolean isDurable() {
		return durable;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public boolean isAutoDelete() {
		return autoDelete;
	}

	public void setAutoDelete(boolean autoDelete) {
		this.autoDelete = autoDelete;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + id;
		result = prime * result + ((exchangeName == null) ? 0 : exchangeName.hashCode());
		result = prime * result + ((serverCfg == null) ? 0 : serverCfg.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ExchangeCfg))
			return false;
		ExchangeCfg other = (ExchangeCfg) obj;
		if (exchangeName == null) {
			if (other.exchangeName != null)
				return false;
		} else if (!exchangeName.equals(other.exchangeName))
			return false;
		if (id != other.id)
			return false;
		if (serverCfg == null) {
			if (other.serverCfg != null)
				return false;
		} else if (!serverCfg.equals(other.serverCfg))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"ExchangeCfg\",id:").append(id).append(", exchangeName:").append(exchangeName).append(", durable:").append(durable).append(", autoDelete:").append(autoDelete).append(", type:").append(type).append(", serverCfg:").append(serverCfg).append("}");
		return builder.toString();
	}
	
	
}
