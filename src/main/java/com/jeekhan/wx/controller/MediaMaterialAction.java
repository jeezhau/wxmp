package com.jeekhan.wx.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.jeekhan.wx.api.MediaMaterialHandle;
import com.jeekhan.wx.dto.Operator;
import com.jeekhan.wx.model.MediaMaterial;
import com.jeekhan.wx.service.MediaMaterialService;
import com.jeekhan.wx.utils.FileFilter;
import com.jeekhan.wx.utils.PageCond;


/**
 * 多媒体素材管理
 * @author jeekhan
 *
 */
@Controller
@RequestMapping("/material")
@SessionAttributes({"operator"})
public class MediaMaterialAction {
	
	@Value("${sys.tmp-file-dir}")
	private String tmpFileDir;	//临时文件保存目录
	
	@Value("${sys.material-file-dir}")
	private String materialFileDir;	//素材文件保存目录
	
	@Autowired
	private MediaMaterialHandle mediaMaterialHandle;
	@Autowired
	private MediaMaterialService mediaMaterialService;
	
	/**
	 * 获取多媒体素材管理主页
	 * @param map
	 * @return
	 */
	@RequestMapping("/index")
	public String getIndex(ModelMap map) {
		
		return "page-material";
	}
	
	/**
	 * 新增临时素材，成功返回后保存
	 * 媒体文件在后台保存时间为3天，即3天后media_id失效
	 * 图片（image）: 2M，支持bmp/png/jpeg/jpg/gif格式 
	 * 语音（voice）：2M，播放长度不超过60s，支持AMR\MP3格式 
	 * 视频（video）：10MB，支持MP4格式 
	 * 缩略图（thumb）：64KB，支持JPG格式
	 * @param file
	 * @param type	媒体文件类型，分别有图片（image）、语音（voice）、视频（video）和缩略图（thumb） 
	 * @return	
	 * {
	 *  "errcode":0,"errmsg":"ok",
	 *  "type":"TYPE","media_id":"MEDIA_ID","created_at":123456789
	 * }
	 * @throws JSONException 
	 */
	public JSONObject addTempMedia(@RequestParam(value="media")MultipartFile media,String type,Operator operator) throws JSONException{
		JSONObject jsonRet;
		if(type == null || type.trim().length() == 0 ||
				media == null || media.isEmpty() ) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -666);
			jsonRet.put("errmsg", "媒体类型、媒体文件不可为空！");
			return jsonRet;
		}
		if(!"image".equals(type) && !"voice".equals(type) && !"video".equals(type) && !"thumb".equals(type)) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "媒体类型只可为图片（image）、语音（voice）、视频（video）、缩略图（thumb）！");
			return jsonRet;
		}
		//文件大小类型判断
		String ctype = media.getOriginalFilename().substring(media.getOriginalFilename().lastIndexOf('.')+1).toLowerCase();
		if("image".equals(type) && (media.getSize()>2*1024*1024 || (!"bmp".equals(ctype) && !"png".equals(ctype)&& !"jpeg".equals(ctype) && !"jpg".equals(ctype) && !"gif".equals(ctype)))) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "图片（image）最大2M，支持bmp/png/jpeg/jpg/gif格式！");
			return jsonRet;
		}
		if("voice".equals(type) && (media.getSize()>2*1024*1024 || (!"amr".equals(ctype) && !"mp3".equals(ctype)))) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "语音（voice）最大2M，播放长度不超过60s，支持AMR、MP3格式！");
			return jsonRet;
		}
		if("video".equals(type) && (media.getSize()>2*1024*1024 || (!"mp4".equals(ctype)))) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "视频（video）最大10MB，支持MP4格式！");
			return jsonRet;
		}
		if("thumb".equals(type) && (media.getSize()>2*1024*1024 || (!"jpg".equals(ctype)))) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "缩略图（thumb）最大64KB，支持JPG格式！");
			return jsonRet;
		}

		File tmpDir = new File(this.tmpFileDir);
		if(!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
		File tmpMedia = new File(tmpDir,media.getOriginalFilename());
		try {
			FileUtils.copyInputStreamToFile(media.getInputStream(), tmpMedia);
			jsonRet = mediaMaterialHandle.addTempMedia(tmpMedia, type);
			if(jsonRet.has("media_id")) {
				jsonRet.put("errcode", 0);
				jsonRet.put("errmsg", "ok");
				//保存于本地
				File materialDir = new File(this.materialFileDir);
				if(!materialDir.exists()) {
					materialDir.mkdirs();
				}
				String mediaId = jsonRet.getString("media_id");
				File file = new File(materialDir,"TEMP_TP" + type + "_" + UUID.randomUUID().toString() + "." + ctype);
				FileUtils.copyFile(tmpMedia, file);
				MediaMaterial material = new MediaMaterial();
				material.setContent(file.getName());
				material.setIsNewsImg("0");
				material.setIsTemp("1");
				material.setMediaId(mediaId);
				material.setMediaType(type);
				material.setMediaUrl("  ");
				material.setStatus("1");
				material.setUpdateUser(operator.getLoginUserId());
				this.mediaMaterialService.add(material);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -777);
			jsonRet.put("errmsg", "系统异常，异常信息：" + e.getMessage());
		}finally {
			tmpMedia.delete();
		}
		return jsonRet;
	}
	
	/**
	 * 上传图文消息内的图片，成功返回后保存
	 * 本接口所上传的图片不占用公众号的素材库中图片数量的5000个的限制。图片仅支持jpg/png格式，大小必须在1MB以下
	 * @param media 图片文件
	 * @return 
	 * {
	 *  "errcode":0,"errmsg":"ok",
	 *  "url":"http://mmbiz.qpic.cn/mmbiz/gLO17UPS6FS2xsypf378iaNhWacZ1G1UplZYWEYfwvuU6Ont96b1roYs CNFwaRrSaKTPCUdBK9DgEHicsKwWCBRQ/0"
	 * }
	 * @throws JSONException 
	 */
	public JSONObject addNewsImg(@RequestParam(value="media")MultipartFile media,Operator operator) throws JSONException{
		JSONObject jsonRet;
		if(media == null || media.isEmpty() ) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -666);
			jsonRet.put("errmsg", "图片文件不可为空！");
			return jsonRet;
		}
		//文件大小类型判断
		String ctype = media.getOriginalFilename().substring(media.getOriginalFilename().lastIndexOf('.')+1).toLowerCase();
		if(media.getSize()>1*1024*1024 || (!"jpg".equals(ctype) && !"png".equals(ctype))) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "图片最大1M，支持png/jpg格式！");
			return jsonRet;
		}
		
		File dir = new File(this.tmpFileDir);
		if(!dir.exists()) {
			dir.mkdirs();
		}
		File tmpMedia = new File(dir,media.getOriginalFilename());
		try {
			FileUtils.copyInputStreamToFile(media.getInputStream(), tmpMedia);
			jsonRet = mediaMaterialHandle.addNewsImg(tmpMedia);
			if(!jsonRet.has("errcode")) {
				jsonRet.put("errcode", 0);
				jsonRet.put("errmsg", "ok");
			}
			File materialDir = new File(this.materialFileDir);
			if(!materialDir.exists()) {
				materialDir.mkdirs();
			}
			if(jsonRet.has("url")) {
				//String mediaId = jsonRet.getString("media_id");
				File file = new File(materialDir,"TEMP_TPimage" + "_" + UUID.randomUUID().toString() + "." + ctype);
				FileUtils.copyFile(tmpMedia, file);
				MediaMaterial material = new MediaMaterial();
				material.setContent(file.getName());
				material.setIsNewsImg("1");
				material.setIsTemp("1");
				material.setMediaId(" ");
				material.setMediaType("image");
				material.setMediaUrl(jsonRet.getString("url"));
				material.setStatus("1");
				material.setUpdateUser(operator.getLoginUserId());
				this.mediaMaterialService.add(material);
			}
		} catch (Exception e) {
			e.printStackTrace();
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -777);
			jsonRet.put("errmsg", "系统异常，异常信息：" + e.getMessage());
		}finally {
			tmpMedia.delete();
		}
		return jsonRet;
	}
	
	/**
	 * 上传图文消息素材，成功返回后保存
	 * @param jsonNews（articles） 图文消息json数组字符串，一个图文消息支持1到8条图文 
	 * 	thumb_media_id ：图文消息缩略图的media_id，可以在基础支持-上传多媒体文件接口中获得
	 * 	author ：图文消息的作者，可为空
	 * 	title ：图文消息的标题 
	 * 	content_source_url ：在图文消息页面点击“阅读原文”后的页面 ，可为空
	 * 	content：图文消息页面的内容，支持HTML标签。具备微信支付权限的公众号，可以使用a标签，其他公众号不能使用，如需插入小程序卡片 
	 * 	digest ：图文消息的描述 ，可为空
	 * 	show_cover_pic：是否显示封面，1为显示，0为不显示 ，可为空
	 * @return
	 *  {
	 *   "errcode":0,"errmsg":"ok",
	 *   "type":"news","media_id":"CsEf3ldqkAYJAU6EJeIkStVDSvffUJ54vqbThMgplD-VJXXof6ctX5fI6-aYyUiQ","created_at":1391857799
	 *  }
	 * @throws JSONException 
	 */
	public JSONObject addNews(String jsonNews,Operator operator) throws JSONException{
		JSONObject jsonRet;
		try {
			JSONObject jsonObj = new JSONObject(jsonNews);
			if(jsonNews == null || jsonNews.isEmpty() ) {
				jsonRet = new JSONObject();
				jsonRet.put("errcode", -666);
				jsonRet.put("errmsg", "图文消息json数组字符串不可为空！格式：{articles:[{}]}");
				return jsonRet;
			}
			JSONArray articles = jsonObj.getJSONArray("articles");
			if(articles == null) {
				jsonRet = new JSONObject();
				jsonRet.put("errcode", -666);
				jsonRet.put("errmsg", "图文消息格式：{articles:[{}]}");
				return jsonRet;
			}
			if(articles.length()<1 || articles.length()>8) {
				jsonRet = new JSONObject();
				jsonRet.put("errcode", -666);
				jsonRet.put("errmsg", "一个图文消息支持1到8条图文！");
				return jsonRet;
			}
			//格式验证
			for(int i=0;i<articles.length();i++) {
				JSONObject item = articles.getJSONObject(i);
				if(item == null || !item.has("thumb_media_id") || !item.has("author") || !item.has("title") || !item.has("content_source_url") || 
						!item.has("content") || !item.has("digest") || !item.has("show_cover_pic")) {
					jsonRet = new JSONObject();
					jsonRet.put("errcode", -666);
					jsonRet.put("errmsg", "图文消息格式：{articles:[{\"thumb_media_id\":\"\",\"author\":\"\",\"title\":\"\",content_source_url:\"\",\"content\":\"\" ,\"digest\":\"\" ,\"show_cover_pic\":\"\"}]}");
					return jsonRet;
				}
			}
			jsonRet = mediaMaterialHandle.addNews(jsonNews);
			if(!jsonRet.has("errcode")) {
				jsonRet.put("errcode", 0);
				jsonRet.put("errmsg", "ok");
			}
			if(jsonRet.has("media_id")) {
				String mediaId = jsonRet.getString("media_id");
				MediaMaterial material = new MediaMaterial();
				material.setContent(jsonNews);
				material.setIsNewsImg("0");
				material.setIsTemp("1");
				material.setMediaId(mediaId);
				material.setMediaType("news");
				material.setMediaUrl("");
				material.setStatus("1");
				material.setUpdateUser(operator.getLoginUserId());
				this.mediaMaterialService.add(material);
			}
		}catch(Exception e) {
			e.printStackTrace();
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -777);
			jsonRet.put("errmsg", "系统异常，异常信息：" + e.getMessage());
		}
		return jsonRet;
	}
	
	/**
	 * 新增永久图文素材
	 * 若新增的是多图文素材，则此处应有几段article结构
	 * @param jsonNews(articles) 图文消息json数组字符串，一个图文消息支持1到8条图文
	 * 	title 			标题 
	 * 	thumb_media_id	图文消息的封面图片素材id（必须是永久mediaID）
	 * 	author 			作者
	 * 	digest 			图文消息的摘要，仅有单图文消息才有摘要，多图文此处为空
	 * 	show_cover_pic	是否显示封面，0为false，即不显示，1为true，即显示
	 *  content 		图文消息的具体内容，支持HTML标签，必须少于2万字符，小于1M，且此处会去除JS 
	 *  content_source_url 	图文消息的原文地址，即点击“阅读原文”后的URL 
	 * @return 
	 * {
	 *  "errcode":0,"errmsg":"ok",
	 *  "media_id":MEDIA_ID
	 * }
	 * @throws JSONException 
	 */
	public JSONObject addPermanentNews(String jsonNews,Operator operator) throws JSONException{
		JSONObject jsonRet;
		try {
			JSONObject jsonObj = new JSONObject(jsonNews);
			if(jsonNews == null || jsonNews.isEmpty() ) {
				jsonRet = new JSONObject();
				jsonRet.put("errcode", -666);
				jsonRet.put("errmsg", "图文消息json数组字符串不可为空！格式：{articles:[{}]}");
				return jsonRet;
			}
			JSONArray articles = jsonObj.getJSONArray("articles");
			if(articles == null) {
				jsonRet = new JSONObject();
				jsonRet.put("errcode", -666);
				jsonRet.put("errmsg", "图文消息格式：{articles:[{}]}");
				return jsonRet;
			}
			if(articles.length()<1 || articles.length()>8) {
				jsonRet = new JSONObject();
				jsonRet.put("errcode", -666);
				jsonRet.put("errmsg", "一个图文消息支持1到8条图文！");
				return jsonRet;
			}
			//格式验证
			for(int i=0;i<articles.length();i++) {
				JSONObject item = articles.getJSONObject(i);
				if(item == null || !item.has("thumb_media_id") || !item.has("author") || !item.has("title") || !item.has("content_source_url") || 
						!item.has("content") || !item.has("digest") || !item.has("show_cover_pic")) {
					jsonRet = new JSONObject();
					jsonRet.put("errcode", -666);
					jsonRet.put("errmsg", "图文消息格式：{articles:[{\"thumb_media_id\":\"\",\"author\":\"\",\"title\":\"\",content_source_url:\"\",\"content\":\"\" ,\"digest\":\"\" ,\"show_cover_pic\":\"\"}]}");
					return jsonRet;
				}
			}
			jsonRet = mediaMaterialHandle.addPermanentNews(jsonNews);
			if(jsonRet.has("media_id")) {
				jsonRet.put("errcode", 0);
				jsonRet.put("errmsg", "ok");
				
				String mediaId = jsonRet.getString("media_id");
				MediaMaterial material = new MediaMaterial();
				material.setContent(jsonNews);
				material.setIsNewsImg("0");
				material.setIsTemp("0");
				material.setMediaId(mediaId);
				material.setMediaType("news");
				material.setMediaUrl("");
				material.setStatus("1");
				material.setUpdateUser(operator.getLoginUserId());
				this.mediaMaterialService.add(material);
			}
		}catch(Exception e) {
			e.printStackTrace();
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -777);
			jsonRet.put("errmsg", "系统异常，异常信息：" + e.getMessage());
		}
		return jsonRet;
	}
	
	
	/**
	 * 新增其他类型永久素材
	 * @param media 媒体文件
	 * @param type 媒体文件类型，分别有图片（image）、语音（voice）和缩略图（thumb）
	 * @return {
	 *  "errcode":0,"errmsg":"ok"
	 *  "media_id":MEDIA_ID,
	 *  "url":URL
	 *  }
	 * @throws JSONException 
	 */
	public JSONObject addPernamentMedia(@RequestParam(value="media")MultipartFile media ,String type,Operator operator) throws JSONException{
		JSONObject jsonRet;
		if(type == null || type.trim().length() == 0 ||
				media == null || media.isEmpty() ) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -666);
			jsonRet.put("errmsg", "媒体类型、媒体文件不可为空！");
			return jsonRet;
		}
		if(!"image".equals(type) && !"voice".equals(type) && !"thumb".equals(type)) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "媒体类型只可为图片（image）、语音（voice）、缩略图（thumb）！");
			return jsonRet;
		}
		//文件大小类型判断
		String ctype = media.getOriginalFilename().substring(media.getOriginalFilename().lastIndexOf('.')+1).toLowerCase();
		if("image".equals(type) && (media.getSize()>2*1024*1024 || (!"bmp".equals(ctype) && !"png".equals(ctype)&& !"jpeg".equals(ctype) && !"jpg".equals(ctype) && !"gif".equals(ctype)))) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "图片（image）最大2M，支持bmp/png/jpeg/jpg/gif格式！");
			return jsonRet;
		}
		if("voice".equals(type) && (media.getSize()>2*1024*1024 || (!"amr".equals(ctype) && !"mp3".equals(ctype)))) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "语音（voice）最大2M，播放长度不超过60s，支持AMR、MP3格式！");
			return jsonRet;
		}
		if("thumb".equals(type) && (media.getSize()>2*1024*1024 || (!"jpg".equals(ctype)))) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "缩略图（thumb）最大64KB，支持JPG格式！");
			return jsonRet;
		}

		File tmpDir = new File(this.tmpFileDir);
		if(!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
		File tmpMedia = new File(tmpDir,media.getOriginalFilename());
		try {
			FileUtils.copyInputStreamToFile(media.getInputStream(), tmpMedia);
			jsonRet = mediaMaterialHandle.addTempMedia(tmpMedia, type);
			if(jsonRet.has("media_id")) {
				jsonRet.put("errcode", 0);
				jsonRet.put("errmsg", "ok");
				
				File materialDir = new File(this.materialFileDir);
				if(!materialDir.exists()) {
					materialDir.mkdirs();
				}
				String mediaId = jsonRet.getString("media_id");
				File file = new File(materialDir,"PERM_TP" + type + "_" + UUID.randomUUID().toString() + "." + ctype);
				FileUtils.copyFile(tmpMedia, file);
				
				MediaMaterial material = new MediaMaterial();
				material.setContent(file.getName());
				material.setIsNewsImg("0");
				material.setIsTemp("0");
				material.setMediaId(mediaId);
				material.setMediaType(type);
				material.setMediaUrl(jsonRet.getString("url"));
				material.setStatus("1");
				material.setUpdateUser(operator.getLoginUserId());
				this.mediaMaterialService.add(material);
			}
		} catch (Exception e) {
			e.printStackTrace();
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -777);
			jsonRet.put("errmsg", "系统异常，异常信息：" + e.getMessage());
		}finally {
			tmpMedia.delete();
		}
		return jsonRet;
	}
	
	/**
	 * 新增视频永久素材，成功后保存
	 * @param media			视频文件
	 * @param title			视频素材的标题
	 * @param introduction	视频素材的描述 
	 * @return {
	 *  "errcode":0,"errmsg":"ok"
	 *  "media_id":MEDIA_ID,
	 *  "url":URL
	 *  }
	 * @throws JSONException 
	 */
	public JSONObject addPernamentVideo(@RequestParam(value="media")MultipartFile media,String title,String introduction,Operator operator) throws JSONException{
		JSONObject jsonRet;
		if(media == null || media.isEmpty() || title == null || title.trim().length()<1 || introduction == null || introduction.trim().length()<1) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -666);
			jsonRet.put("errmsg", "视频文件、视频素材的标题、视频素材的描述不可为空！");
			return jsonRet;
		}

		//文件大小类型判断
		String ctype = media.getOriginalFilename().substring(media.getOriginalFilename().lastIndexOf('.')+1).toLowerCase();
		if(media.getSize()>2*1024*1024 || (!"mp4".equals(ctype))) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "视频（video）最大10MB，支持MP4格式！");
			return jsonRet;
		}

		File tmpDir = new File(this.tmpFileDir);
		if(!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
		File tmpMedia = new File(tmpDir,media.getOriginalFilename());
		try {
			FileUtils.copyInputStreamToFile(media.getInputStream(), tmpMedia);
			jsonRet = mediaMaterialHandle.addPernamentVideo(tmpMedia, title, introduction);
			if(jsonRet.has("media_id")) {
				jsonRet.put("errcode", 0);
				jsonRet.put("errmsg", "ok");
			
				File materialDir = new File(this.materialFileDir);
				if(!materialDir.exists()) {
					materialDir.mkdirs();
				}
				String mediaId = jsonRet.getString("media_id");
				File file = new File(materialDir,"PERM_TPvideo" + "_" + UUID.randomUUID().toString() + "." + ctype);
				FileUtils.copyFile(tmpMedia, file);
				
				MediaMaterial material = new MediaMaterial();
				material.setContent(file.getName());
				material.setIsNewsImg("0");
				material.setIsTemp("0");
				material.setMediaId(mediaId);
				material.setMediaType("video");
				material.setMediaUrl(jsonRet.getString("url"));
				material.setStatus("1");
				material.setUpdateUser(operator.getLoginUserId());
				this.mediaMaterialService.add(material);
			}
		} catch (Exception e) {
			e.printStackTrace();
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -777);
			jsonRet.put("errmsg", "系统异常，异常信息：" + e.getMessage());
		}finally {
			tmpMedia.delete();
		}
		return jsonRet;
	}
	
	
	/**
	 * 获取图文素材，从本地获取
	 * @param mediaId	素材ID
	 * @param isTemp		是否临时
	 * @return 图文信息
	 * @throws JSONException 
	 */
	@RequestMapping("/getnews/{mediaId}/{isTemp}")
	@ResponseBody
	public String getNews(@PathVariable("mediaId")String mediaId,@PathVariable("isTemp")String isTemp) throws JSONException{
		JSONObject jsonRet = new JSONObject();
		try {
			Map<String,Object> condParams = new HashMap<String,Object>();
			//条件整合
			condParams.put("isTemp", isTemp);
			condParams.put("mediaId", mediaId);

			List<MediaMaterial> datas = this.mediaMaterialService.getAll(condParams, new PageCond(0,1));
			jsonRet.put("datas", new JSONArray(datas));
			jsonRet.put("errcode", 0);
			jsonRet.put("errmsg", "ok");
		}catch(Exception e) {
			e.printStackTrace();
			jsonRet.put("errcode", -999);
			jsonRet.put("errmsg","系统异常，异常信息：" + e.getMessage());
		}
		return jsonRet.toString();
	}
	
	/**
	 * 显示二维码图片
	 * @param picName	图片名称
	 * @param out
	 * @throws IOException 
	 */
	@RequestMapping(value="/getmedia/{filename}")
	public void showMedia(@PathVariable("filename")String filename,OutputStream out,HttpServletRequest request,HttpServletResponse response) throws IOException{
		File file = new File(this.materialFileDir,filename);
		FileInputStream fis = new FileInputStream(file);
		try {
			Map<String,Object> condParams = new HashMap<String,Object>();
			condParams.put("filename", filename);
			List<MediaMaterial> datas = this.mediaMaterialService.getAll(condParams, new PageCond(0,1));
			if(datas == null || datas.size()<1) {
				return;
			}
			MediaMaterial material = datas.get(0);
			String mediaType = material.getMediaType();
			if("image".equals(mediaType) || "thumb".equals(mediaType)) {
				response.setContentType("image/*");
				BufferedImage image = ImageIO.read(file);
				OutputStream os = response.getOutputStream();  
				String type = file.getName().substring(file.getName().lastIndexOf('.')+1);
				ImageIO.write(image, type, os);  
				return;
			}
			if("voice".equals(mediaType)) {
				response.setContentType("audio/*");
				byte[] buff = new byte[1024];
				
				int length = 0;
				while((length = fis.read(buff)) != -1) {
					out.write(buff, 0, length);
				}
				return;
			}
			if("video".equals(mediaType)) {
				response.setContentType("video/*");
				byte[] buff = new byte[1024];
				
				int length = 0;
				while((length = fis.read(buff)) != -1) {
					out.write(buff, 0, length);
				}
				return;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			fis.close();
		}
	}
	
	/**
	 * 删除永久素材
	 * @param mediaId
	 * @return {"errcode":ERRCODE,"errmsg":ERRMSG}
	 * @throws JSONException 
	 */
	public JSONObject deletePermanentMedia(String mediaId) throws JSONException{
		JSONObject jsonRet;
		if(mediaId == null || mediaId.trim().length() < 1) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -666);
			jsonRet.put("errmsg", "永久素材ID不可为空！");
			return jsonRet;
		}
		try {
			jsonRet = mediaMaterialHandle.deletePermanentMedia(mediaId);
		} catch (Exception e) {
			e.printStackTrace();
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -777);
			jsonRet.put("errmsg", "系统异常，异常信息：" + e.getMessage());
		}
		return jsonRet;
	}
	
	/**
	 * 修改永久图文素材
	 * @param mediaId	媒体ID
	 * @param index		要更新的文章在图文消息中的位置（多图文消息时，此字段才有意义），第一篇为0 
	 * @param jsonActicle	文章信息json对象
	 * 	title 			标题 
	 * 	thumb_media_id	图文消息的封面图片素材id（必须是永久mediaID）
	 * 	author 			作者
	 * 	digest 			图文消息的摘要，仅有单图文消息才有摘要，多图文此处为空
	 * 	show_cover_pic	是否显示封面，0为false，即不显示，1为true，即显示
	 *  content 		图文消息的具体内容，支持HTML标签，必须少于2万字符，小于1M，且此处会去除JS 
	 *  content_source_url 	图文消息的原文地址，即点击“阅读原文”后的URL 
	 * @return  {"errcode":ERRCODE,"errmsg":ERRMSG}
	 * @throws JSONException 
	 */
	public JSONObject updatePermanentNews(String mediaId,Integer index,String jsonActicle) throws JSONException{
		JSONObject jsonRet;
		if(mediaId == null || mediaId.trim().length()<1 || jsonActicle == null || jsonActicle.trim().length()<1 || index == null || index<0 || index >7) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -666);
			jsonRet.put("errmsg", "永久图文素材ID、文章信息、文章序号（0-7）不可为空！");
			return jsonRet;
		}
		try {
			//格式验证
			JSONObject article = new JSONObject(jsonActicle);
			if(article == null || !article.has("thumb_media_id") || !article.has("author") || !article.has("title") || !article.has("content_source_url") || 
					!article.has("content") || !article.has("digest") || !article.has("show_cover_pic")) {
				jsonRet = new JSONObject();
				jsonRet.put("errcode", -666);
				jsonRet.put("errmsg", "文章格式：{\"thumb_media_id\":\"\",\"author\":\"\",\"title\":\"\",content_source_url:\"\",\"content\":\"\" ,\"digest\":\"\" ,\"show_cover_pic\":\"\"}");
				return jsonRet;
			}
			jsonRet = mediaMaterialHandle.updatePermanentNews(mediaId, index, article);
			JSONObject news = mediaMaterialHandle.getPermanentNews(mediaId);
			File file = new File(this.materialFileDir,"PERM_TPnews" + "_" + mediaId + ".txt");
			JSONObject tmp = new JSONObject();
			tmp.put("articles", news.get("news_item"));
			FileUtils.write(file, tmp.toString(), "utf8");
		}catch(Exception e) {
			e.printStackTrace();
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -777);
			jsonRet.put("errmsg", "系统异常，异常信息：" + e.getMessage());
		}
		return jsonRet;
	}
	
	/**
	 * 获取素材总数
	 * @param type		素材的类型，图片（image）、视频（video）、语音 （voice）、图文（news）、缩略图（thumb）
	 * @param isTemp		是否临时素材
	 * @return 
	 * {
	 *  "errcode":0,"errmsg":"ok",
	 *  "voice_count":COUNT,"video_count":COUNT,"image_count":COUNT, "news_count":COUNT
	 * }
	 * @throws JSONException 
	 */
	public JSONObject getMediaCount(String type,boolean isTemp) throws JSONException{
		JSONObject jsonRet = new JSONObject();
		if(type == null || type.trim().length() == 0 ) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -666);
			jsonRet.put("errmsg", "素材的类型不可为空！");
			return jsonRet;
		}
		if(!"image".equals(type) && !"voice".equals(type) && !"video".equals(type) && !"news".equals(type) && !"thumb".equals(type)) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "媒体类型只可为图片（image）、语音（voice）、视频（video）、图文（news）！");
			return jsonRet;
		}
		File materialDir = new File(this.materialFileDir);
		int voice_count = 0;
		File[] files;
		files = materialDir.listFiles(new FileFilter("PERM" + "_TPvoice"));
		if(files != null) {
			voice_count = files.length;
		}
		jsonRet.put("voice_count", voice_count);
		
		int video_count = 0 ;
		files = materialDir.listFiles(new FileFilter("PERM" + "_TPvideo"));
		if(files != null) {
			video_count = files.length;
		}
		jsonRet.put("video_count", video_count);
		
		int image_count = 0;
		files = materialDir.listFiles(new FileFilter("PERM" + "_TPimage"));
		if(files != null) {
			image_count = files.length;
		}
		jsonRet.put("image_count", image_count);
		
		int news_count = 0;
		files = materialDir.listFiles(new FileFilter("PERM" + "_TPnews"));
		if(files != null) {
			news_count = files.length;
		}
		jsonRet.put("news_count", news_count);
		jsonRet.put("errcode", 0);
		jsonRet.put("errmsg", "ok");
		return jsonRet;
	}
	
	/**
	 * 获取素材列表
	 * @param type		素材的类型，图片（image）、视频（video）、语音 （voice）、图文（news）、缩略图（thumb）
	 * @param isTemp		是否临时素材 
	 * @param offset		从全部素材的该偏移位置开始返回，0表示从第一个素材 返回 
	 * @return
	 * @throws JSONException 
	 */
	public JSONObject getMediaList(String type,boolean isTemp,Integer offset) throws JSONException{
		JSONObject jsonRet;
		if(type == null || type.trim().length() == 0 || offset == null || offset<0 ) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -666);
			jsonRet.put("errmsg", "素材的类型、偏移位置不可为空！");
			return jsonRet;
		}
		if(!"image".equals(type) && !"voice".equals(type) && !"video".equals(type) && !"news".equals(type) && !"thumb".equals(type)) {
			jsonRet = new JSONObject();
			jsonRet.put("errcode", -888);
			jsonRet.put("errmsg", "媒体类型只可为图片（image）、语音（voice）、视频（video）、图文（news）！");
			return jsonRet;
		}
		return null;
	}
	
	/**
	 * 
	 * @param jsonParams 查询条件JSON格式
	 * @param pageCond	分页信息
	 * @return
	 * @throws JSONException 
	 */
	@RequestMapping("/search")
	@ResponseBody
	public String searchAll(String jsonParams,PageCond pageCond) throws JSONException {
		JSONObject jsonRet = new JSONObject();
		try {
			Map<String,Object> condParams = new HashMap<String,Object>();
			if(jsonParams != null) {
				JSONObject params = new JSONObject(jsonParams);
				//条件整合
				if(params.has("isTemp")) {
					if(params.getString("isTemp") != null && (params.getString("isTemp").trim().length()>0)){
						condParams.put("isTemp", params.getString("isTemp"));
					}
				}
				if(params.has("mediaType")) {
					if(params.getString("mediaType") != null && (params.getString("mediaType").trim().length()>0)){
						condParams.put("mediaType", params.getString("mediaType"));
					}
				}
				if(params.has("isNewsImg")) {
					if(params.getString("isNewsImg") != null && (params.getString("isNewsImg").trim().length()>0)){
						condParams.put("isNewsImg", params.getString("isNewsImg"));
					}
				}
				if(params.has("beginTime")) {
					if(params.getString("beginTime") != null && (params.getString("beginTime").trim().length()>0)){
						condParams.put("beginTime", params.getString("beginTime"));
					}
				}
				if(params.has("endTime")) {
					if(params.getString("endTime") != null && (params.getString("endTime").trim().length()>0)){
						condParams.put("endTime", params.getString("endTime"));
					}
				}
			}
			if(pageCond == null) {
				pageCond = new PageCond(0,20);
			}
			if(pageCond.getBegin()<0) {
				pageCond.setBegin(0);
			}
			if(pageCond.getPageSize() < 2 || pageCond.getPageSize() > 100) {
				pageCond.setPageSize(20);
			}
			
			int cnt = this.mediaMaterialService.countAll(condParams);
			pageCond.setCount(cnt);
			if(cnt > 0) {
			List<MediaMaterial> datas = this.mediaMaterialService.getAll(condParams, pageCond);
				jsonRet.put("datas", new JSONArray(datas));
			}
			JSONObject jaonPage = new JSONObject();
			jaonPage.put("pageSize", pageCond.getPageSize());
			jaonPage.put("begin", pageCond.getBegin());
			jaonPage.put("count", pageCond.getCount());
			jsonRet.put("errcode", 0);
			jsonRet.put("errmsg", "ok");
			jsonRet.put("pageCond", jaonPage);
		}catch(Exception e) {
			e.printStackTrace();
			jsonRet.put("errcode", -999);
			jsonRet.put("errmsg","系统异常，异常信息：" + e.getMessage());
		}
		return jsonRet.toString();
	}
	
}
