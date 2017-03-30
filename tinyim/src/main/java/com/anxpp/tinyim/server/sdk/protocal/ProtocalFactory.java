package com.anxpp.tinyim.server.sdk.protocal;

import com.anxpp.tinyim.server.sdk.protocal.c.PKeepAlive;
import com.anxpp.tinyim.server.sdk.protocal.s.PErrorResponse;
import com.anxpp.tinyim.server.sdk.protocal.s.PKeepAliveResponse;
import com.anxpp.tinyim.server.sdk.protocal.s.PLoginInfoResponse;
import com.anxpp.tinyim.server.sdk.protocal.c.LoginInfo;
import com.google.gson.Gson;

public class ProtocalFactory
{
	private static String create(Object c)
	{
		return new Gson().toJson(c);
	}

	public static <T> T parse(byte[] fullProtocalJASOnBytes, int len, Class<T> clazz)
	{
		return parse(CharsetHelper.getString(fullProtocalJASOnBytes, len), clazz);
	}

	public static <T> T parse(String dataContentOfProtocal, Class<T> clazz)
	{
		return new Gson().fromJson(dataContentOfProtocal, clazz);
	}

	public static Protocal parse(byte[] fullProtocalJASOnBytes, int len)
	{
		return (Protocal)parse(fullProtocalJASOnBytes, len, Protocal.class);
	}

	public static Protocal createPKeepAliveResponse(int to_user_id)
	{
		return new Protocal(ProtocalType.S.FROM_SERVER_TYPE_OF_RESPONSE$KEEP$ALIVE, 
				create(new PKeepAliveResponse()), 0, to_user_id);
	}

	public static PKeepAliveResponse parsePKeepAliveResponse(String dataContentOfProtocal)
	{
		return (PKeepAliveResponse)parse(dataContentOfProtocal, PKeepAliveResponse.class);
	}

	public static Protocal createPKeepAlive(int from_user_id)
	{
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_KEEP$ALIVE, 
				create(new PKeepAlive()), from_user_id, 0);
	}

	public static PKeepAlive parsePKeepAlive(String dataContentOfProtocal)
	{
		return (PKeepAlive)parse(dataContentOfProtocal, PKeepAlive.class);
	}

	public static Protocal createPErrorResponse(int errorCode, String errorMsg, int user_id)
	{
		return new Protocal(ProtocalType.S.FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR, 
				create(new PErrorResponse(errorCode, errorMsg)), 0, user_id);
	}

	public static PErrorResponse parsePErrorResponse(String dataContentOfProtocal)
	{
		return (PErrorResponse)parse(dataContentOfProtocal, PErrorResponse.class);
	}

	public static Protocal createPLoginoutInfo(int user_id, String loginName)
	{
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_LOGOUT
//				, create(new PLogoutInfo(user_id, loginName))
				, null
				, user_id, 0);
	}

	public static Protocal createPLoginInfo(String loginName, String loginPsw, String extra)
	{
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_LOGIN
				, create(new LoginInfo(loginName, loginPsw, extra)), -1, 0);
	}

	public static LoginInfo parsePLoginInfo(String dataContentOfProtocal)
	{
		return (LoginInfo)parse(dataContentOfProtocal, LoginInfo.class);
	}

	public static Protocal createPLoginInfoResponse(int code, int user_id)
	{
		return new Protocal(ProtocalType.S.FROM_SERVER_TYPE_OF_RESPONSE$LOGIN, 
				create(new PLoginInfoResponse(code, user_id)),
				0, 
				user_id, 
				true, Protocal.genFingerPrint());
	}

	public static PLoginInfoResponse parsePLoginInfoResponse(String dataContentOfProtocal)
	{
		return (PLoginInfoResponse)parse(dataContentOfProtocal, PLoginInfoResponse.class);
	}

	public static Protocal createCommonData(String dataContent, int from_user_id, int to_user_id, boolean QoS, String fingerPrint)
	{
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA, 
				dataContent, from_user_id, to_user_id, QoS, fingerPrint);
	}

	public static Protocal createCommonData(String dataContent, int from_user_id, int to_user_id)
	{
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA, 
				dataContent, from_user_id, to_user_id);
	}

	public static Protocal createRecivedBack(int from_user_id, int to_user_id, String recievedMessageFingerPrint)
	{
		return new Protocal(ProtocalType.C.FROM_CLIENT_TYPE_OF_RECEIVED
				, recievedMessageFingerPrint, from_user_id, to_user_id);// 该包当然不需要QoS支持！
	}
}