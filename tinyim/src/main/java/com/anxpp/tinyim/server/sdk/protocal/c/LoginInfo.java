package com.anxpp.tinyim.server.sdk.protocal.c;

public class LoginInfo
{
	private String username = null;
	private String password = null;
	private String extra = null;

	public LoginInfo(String username, String password, String extra)
	{
		this.username = username;
		this.password = password;
		this.extra = extra;
	}

	public String getUsername()
	{
		return this.username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return this.password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}
	
	public String getExtra()
	{
		return extra;
	}

	public void setExtra(String extra)
	{
		this.extra = extra;
	}
}