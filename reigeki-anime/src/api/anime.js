/**
 * 公开数据接口（首页用，无需登录）。
 */

import { http } from './request.js'

/**
 * 首页「本周热门番剧」数据源：只含上架记录，按 sort 升序。
 * @returns {Promise<Array<{id:number,title:string,description:string,tag:string,
 *   rating:string,episode:number,cover:string,sort:number,visible:boolean}>>}
 */
export function fetchPublicAnimes() {
  return http.get('/api/animes')
}

/**
 * 单部番剧详情（播放页头部用）。
 * @param {number} id 番剧 id
 */
export function fetchAnime(id) {
  return http.get(`/api/animes/${id}`)
}

/**
 * 某部番剧的剧集列表，按集数升序。
 * @param {number} animeId 番剧 id
 * @returns {Promise<Array<{id:number,animeId:number,episodeNo:number,title:string,
 *   videoPath:string,durationSeconds:number|null}>>}
 */
export function fetchEpisodes(animeId) {
  return http.get(`/api/animes/${animeId}/episodes`)
}

/**
 * 视频流地址。video 标签直接用，Range 分段由浏览器自己处理。
 * @param {number} episodeId 剧集 id
 */
export function videoUrl(episodeId) {
  return `/api/videos/${episodeId}`
}
