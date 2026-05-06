<template>
  <el-config-provider>
    <el-container class="app-container">
      <el-aside width="240px" class="sidebar">
        <div class="logo">GalSpace</div>

        <div class="avatar-container">
          <div class="avatar-placeholder">
            <el-icon :size="50" color="#5b8def"><UserFilled /></el-icon>
          </div>
        </div>

        <el-menu default-active="all" class="sidebar-menu" :router="false">
          <el-menu-item index="all" @click="setFilter('all')">
            <span class="menu-title">全部游戏</span>
            <span class="badge">{{ games.length }}</span>
          </el-menu-item>
          <el-sub-menu index="tag">
            <template #title><span class="menu-title">TAG</span></template>
            <el-menu-item v-for="t in allTags" :key="t[0]" :index="'tag-'+t[0]" @click="setFilter('tag',t[1])">{{ t[0] }}</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="dev">
            <template #title><span class="menu-title">制作组</span></template>
            <el-menu-item v-for="d in allDevs" :key="d" :index="'dev-'+d" @click="setFilter('dev',d)">{{ d }}</el-menu-item>
          </el-sub-menu>
        </el-menu>

        <div class="bottom-menu">
          <div class="menu-item" :class="{ 'is-active': activeFilter.type === 'favorite' }" @click="setFilter('favorite')">
            <el-icon :color="activeFilter.type === 'favorite' ? '#e6a23c' : ''"><StarFilled /></el-icon>
            <span class="menu-title">收藏夹</span>
            <span class="badge">{{ favoriteCount }}</span>
          </div>
          <div class="menu-item category-row" v-for="c in categories" :key="c.id" :class="{ 'is-active': activeFilter.type === 'category' && activeFilter.value === c.name }" @click="setFilter('category', c.name)">
            <el-icon color="#5b8def"><Collection /></el-icon>
            <span class="menu-title sub-menu-title">{{ c.name }}</span>
            <span class="badge">{{ c.gameCount }}</span>
            <el-icon class="category-delete-icon" :size="14" @click.stop="deleteCategoryConfirm(c)"><Close /></el-icon>
          </div>
          <div class="menu-item add-category-item" @click="showAddCategoryDialog = true">
            <el-icon><Plus /></el-icon>
            <span class="menu-title">新建分类</span>
          </div>
          <div class="menu-item settings-item" @click="showConfigDialog = true">
            <el-icon><Setting /></el-icon>
            <span class="menu-title">设置</span>
          </div>
          <div class="menu-item settings-item exit-item" @click="shutdownServer">
            <el-icon><SwitchButton /></el-icon>
            <span class="menu-title">退出程序</span>
          </div>
        </div>
      </el-aside>

      <el-container class="main-container">
        <el-header class="main-header" height="80px">
          <div class="header-inner">
            <div class="stats-bar">
              <span class="stat-item"><strong>{{ filteredGames.length }}</strong> 款游戏</span>
              <span class="stat-item"><strong>0</strong> 全CG</span>
              <span class="stat-item"><strong>0</strong> 已通关</span>
              <span class="stat-item"><strong>0h</strong> 总游玩</span>
            </div>
            <div class="header-actions">
              <el-input v-model="searchQuery" placeholder="搜索游戏..." class="search-input" clearable>
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-button class="action-btn" @click="showScanDialog = true"><el-icon><FolderOpened /></el-icon> 扫描</el-button>
              <el-button class="action-btn" :class="{ 'is-active-btn': selectMode }" @click="toggleSelectMode"><el-icon><Check /></el-icon> 批量选择</el-button>
              <el-dropdown class="sort-dropdown">
                <el-button class="action-btn">默认排序 <el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item>按添加时间</el-dropdown-item>
                    <el-dropdown-item>按最后运行</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-header>

        <div class="batch-bar" v-if="selectMode && selectedIds.size > 0">
          <span class="batch-count">已选 <strong>{{ selectedIds.size }}</strong> 款游戏</span>
          <div class="batch-actions">
            <el-button size="small" @click="selectAll">全选</el-button>
            <el-button size="small" @click="clearSelection">取消选择</el-button>
            <el-button size="small" type="primary" @click="batchVndbFetch" :loading="batchVndbLoading" color="#002FA7"><el-icon><Download /></el-icon> VNDB 抓取</el-button>
            <el-button size="small" type="danger" @click="batchDelete" :loading="batchDeleting" plain><el-icon><Delete /></el-icon> 批量删除</el-button>
            <el-dropdown trigger="click" @command="batchAddToCategory">
              <el-button size="small" type="success" plain><el-icon><Collection /></el-icon> 添加到分类 <el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="c in categories" :key="c.id" :command="c.id">{{ c.name }}</el-dropdown-item>
                  <el-dropdown-item v-if="categories.length === 0" disabled>暂无分类</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <el-main class="main-content">
          <div v-if="filteredGames.length === 0" class="empty-state">
            <el-empty description="库中还没有游戏，点击右上角扫描或右下角添加吧！" />
          </div>
          <div v-else class="game-grid">
            <div v-for="(game, index) in filteredGames" :key="game.id" class="game-card"
              :class="{ 'is-selected': selectedIds.has(game.id), 'drag-over': dragId && dragId !== game.id }"
              draggable="true"
              @dragstart="onDragStart($event, game.id)"
              @dragover.prevent="onDragOver($event, game.id)"
              @dragleave="onDragLeave($event)"
              @drop="onDrop($event, game.id)"
              @click="selectMode ? toggleSelect(game.id) : openGameDetails(game)">
              <div class="select-check" v-if="selectMode" @click.stop="toggleSelect(game.id)">
                <el-icon :size="18" :color="selectedIds.has(game.id) ? '#002FA7' : '#ccc'">
                  <component :is="selectedIds.has(game.id) ? 'CircleCheckFilled' : 'CircleCheck'" />
                </el-icon>
              </div>
              <div class="card-favorite" v-if="!selectMode" @click.stop="toggleFavorite(game)">
                <el-icon :size="18" :color="game.favorite ? '#e6a23c' : '#ccc'"><StarFilled /></el-icon>
              </div>
              <div class="cover-wrapper">
                <div class="game-number">NO.{{ String(index + 1).padStart(3, '0') }}</div>
                <img :src="game.coverUrl || defaultCover" class="cover-image" :class="{ 'blurred-cover': game.blurred || configForm.globalBlur }" />
                <div class="play-overlay" @click.stop="selectMode ? toggleSelect(game.id) : launchGame(game)">
                  <el-icon class="play-icon"><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="game-info">
                <h3 class="game-title" :title="game.titleZh || game.title">{{ game.titleZh || game.title }}</h3>
                <div class="tags">
                  <span v-for="tag in game.tags.slice(0,3)" :key="tag" class="custom-tag">{{ tag }}</span>
                  <span v-if="game.tags.length === 0" class="custom-tag">ADV</span>
                </div>
              </div>
            </div>
          </div>
        </el-main>

        <div class="fab-button" @click="showAddDialog = true"><el-icon><EditPen /></el-icon></div>
      </el-container>

      <el-dialog v-model="showScanDialog" width="460px" class="custom-dialog" :show-close="false">
        <template #header><div class="custom-dialog-header"><span>扫描本地游戏目录</span></div></template>
        <div class="settings-list">
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#4a7fd4"><FolderOpened /></el-icon><span>目标目录</span></div>
            <div class="settings-item-control" style="gap:8px;">
              <el-input v-model="scanForm.path" placeholder="点击右侧选择..." size="small" style="width:180px;" readonly />
              <el-button size="small" class="pick-btn" @click="pickDirectory('scanForm','path')"><el-icon><Folder /></el-icon></el-button>
            </div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#5b9bd5"><Sort /></el-icon><span>扫描深度</span></div>
            <div class="settings-item-control"><el-input-number v-model="scanForm.depth" :min="1" :max="5" size="small" /></div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#e6a23c"><Aim /></el-icon><span>模糊匹配</span></div>
            <div class="settings-item-control">
              <el-switch v-model="scanForm.fuzzy" style="--el-switch-on-color:#002FA7" />
              <span class="setting-hint">只要文件夹含 .exe 即添加</span>
            </div>
          </div>
        </div>
        <template #footer><div class="custom-dialog-footer">
          <el-button round @click="showScanDialog=false">取消</el-button>
          <el-button round type="primary" @click="startScan" :loading="scanning" color="#002FA7">开始扫描</el-button>
        </div></template>
      </el-dialog>

      <el-dialog v-model="showConfigDialog" width="520px" class="custom-dialog" :show-close="false">
        <template #header><div class="custom-dialog-header"><span>设置</span></div></template>
        <div class="settings-list">
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#002FA7"><Folder /></el-icon><span>LEProc.exe 路径</span></div>
            <div class="settings-item-control" style="gap:8px;">
              <el-input v-model="configForm.leProcPath" placeholder="点击右侧选择..." size="small" style="width:180px;" readonly />
              <el-button size="small" class="pick-btn" @click="pickFile('configForm','leProcPath','.exe')"><el-icon><Folder /></el-icon></el-button>
            </div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#4a7fd4"><Refresh /></el-icon><span>启动时自动扫描</span></div>
            <div class="settings-item-control"><el-switch v-model="configForm.autoScanEnabled" style="--el-switch-on-color:#002FA7" /></div>
          </div>
          <div class="settings-item-row" v-if="configForm.autoScanEnabled">
            <div class="settings-item-label"><el-icon color="#5b9bd5"><Location /></el-icon><span>自动扫描路径</span></div>
            <div class="settings-item-control" style="gap:8px;">
              <el-input v-model="configForm.autoScanPath" placeholder="点击右侧选择..." size="small" style="width:180px;" readonly />
              <el-button size="small" class="pick-btn" @click="pickDirectory('configForm','autoScanPath')"><el-icon><Folder /></el-icon></el-button>
            </div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#5b8def"><Hide /></el-icon><span>全局封面模糊</span></div>
            <div class="settings-item-control"><el-switch v-model="configForm.globalBlur" style="--el-switch-on-color:#002FA7" /></div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#e6a23c"><Key /></el-icon><span>DeepSeek API Key</span></div>
            <div class="settings-item-control"><el-input v-model="configForm.deepseekApiKey" placeholder="输入 API Key" size="small" style="width:240px;" type="password" show-password /></div>
          </div>
          <div class="settings-section-divider"><span>服务器网络设置（重启后生效）</span></div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#5b8def"><Monitor /></el-icon><span>监听地址</span></div>
            <div class="settings-item-control">
              <el-input v-model="configForm.serverAddress" placeholder="0.0.0.0" size="small" style="width:180px;" />
              <span class="setting-hint">0.0.0.0 = 所有网卡</span>
            </div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#67c23a"><Connection /></el-icon><span>监听端口</span></div>
            <div class="settings-item-control"><el-input-number v-model="configForm.serverPort" :min="1024" :max="65535" size="small" style="width:140px;" /></div>
          </div>
        </div>
        <template #footer><div class="custom-dialog-footer">
          <el-button round @click="showConfigDialog=false">取消</el-button>
          <el-button round type="warning" @click="restartServer" :loading="restarting" color="#e6a23c">保存并重启</el-button>
          <el-button round type="primary" @click="saveConfig" color="#002FA7">保存设置</el-button>
        </div></template>
      </el-dialog>

      <el-dialog v-model="showAddCategoryDialog" width="400px" class="custom-dialog" :show-close="false">
        <template #header><div class="custom-dialog-header"><span>新建分类</span></div></template>
        <div class="settings-list">
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#5b8def"><Collection /></el-icon><span>分类名称</span></div>
            <div class="settings-item-control"><el-input v-model="newCategoryName" placeholder="输入分类名" size="small" style="width:160px;" @keyup.enter="createCategory" /></div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#e6a23c"><Brush /></el-icon><span>标识颜色</span></div>
            <div class="settings-item-control" style="gap:8px;">
              <el-color-picker v-model="newCategoryColor" size="small" :predefine="['#002FA7','#5b8def','#67c23a','#e6a23c','#f56c6c','#909399','#409eff','#8b5cf6','#ec4899','#14b8a6']" />
              <span class="setting-hint">{{ newCategoryColor }}</span>
            </div>
          </div>
        </div>
        <template #footer><div class="custom-dialog-footer">
          <el-button round @click="showAddCategoryDialog=false">取消</el-button>
          <el-button round type="primary" @click="createCategory" color="#002FA7">创建</el-button>
        </div></template>
      </el-dialog>

      <el-dialog v-model="showAddDialog" width="460px" class="custom-dialog" :show-close="false">
        <template #header><div class="custom-dialog-header"><span>手动添加游戏</span></div></template>
        <div class="settings-list">
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#002FA7"><EditPen /></el-icon><span>游戏名称</span></div>
            <div class="settings-item-control"><el-input v-model="addForm.title" placeholder="输入游戏原名或译名" size="small" style="width:200px;" /></div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#4a7fd4"><Link /></el-icon><span>启动程序</span></div>
            <div class="settings-item-control" style="gap:8px;">
              <el-input v-model="addForm.exePath" placeholder="点击右侧选择..." size="small" style="width:160px;" readonly />
              <el-button size="small" class="pick-btn" @click="pickFile('addForm','exePath','.exe')"><el-icon><Folder /></el-icon></el-button>
            </div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#5b9bd5"><Location /></el-icon><span>游戏目录</span></div>
            <div class="settings-item-control" style="gap:8px;">
              <el-input v-model="addForm.gameDir" placeholder="点击右侧选择..." size="small" style="width:160px;" readonly />
              <el-button size="small" class="pick-btn" @click="pickDirectory('addForm','gameDir')"><el-icon><Folder /></el-icon></el-button>
            </div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#67c23a"><Switch /></el-icon><span>转区启动</span></div>
            <div class="settings-item-control"><el-switch v-model="addForm.needLocaleEmulator" style="--el-switch-on-color:#002FA7" /></div>
          </div>
        </div>
        <template #footer><div class="custom-dialog-footer">
          <el-button round @click="showAddDialog=false">取消</el-button>
          <el-button round type="primary" @click="manualAddGame" :loading="adding" color="#002FA7">确认添加</el-button>
        </div></template>
      </el-dialog>

      <el-dialog v-model="showDetailDialog" width="85vw" class="custom-dialog detail-dialog" :show-close="false" @closed="gameDetail = null" top="6vh">
        <template #header><div class="custom-dialog-header"><span>游戏详情</span></div></template>
        <div class="detail-container" v-if="detailGame">
          <div class="detail-cover-section">
            <img :src="gameDetail?.coverUrl || detailGame.coverUrl || defaultCover" class="detail-cover" :class="{ 'blurred-cover': detailGame.blurred || configForm.globalBlur }" />
            <div class="detail-rating" v-if="gameDetail?.rating"><span class="rating-score">{{ gameDetail.rating / 10 }}</span><span class="rating-unit">/ 10</span></div>
          </div>
          <div class="detail-info-section">
            <div class="detail-title-row">
              <h2 class="detail-title">{{ gameDetail?.alttitle || detailGame.vndbAlttitle || detailGame.title }}</h2>
              <p class="detail-title-orig" v-if="detailGame.titleZh && detailGame.titleZh !== (gameDetail?.alttitle || detailGame.vndbAlttitle || detailGame.title)">{{ detailGame.titleZh }}</p>
              <p class="detail-title-sub" v-if="detailGame.vndbTitle && detailGame.vndbTitle !== (gameDetail?.alttitle || detailGame.vndbAlttitle)">{{ detailGame.vndbTitle }}</p>
            </div>
            <div class="detail-meta">
              <div class="detail-meta-item" v-if="gameDetail?.developer || detailGame.developer"><el-icon color="#4a7fd4"><OfficeBuilding /></el-icon><span class="meta-label">厂商</span><span class="meta-value">{{ gameDetail?.developer || detailGame.developer }}</span><span class="meta-original" v-if="gameDetail?.developerOriginal && gameDetail.developerOriginal !== (gameDetail?.developer || detailGame.developer)">({{ gameDetail.developerOriginal }})</span></div>
              <div class="detail-meta-item" v-if="gameDetail?.released || detailGame.vndbReleased"><el-icon color="#67c23a"><Calendar /></el-icon><span class="meta-label">发售日</span><span class="meta-value">{{ gameDetail?.released || detailGame.vndbReleased }}</span></div>
              <div class="detail-meta-item" v-if="gameDetail?.lengthMinutes || detailGame.vndbLengthMinutes"><el-icon color="#e6a23c"><Clock /></el-icon><span class="meta-label">时长</span><span class="meta-value">{{ formatMinutes(gameDetail?.lengthMinutes || detailGame.vndbLengthMinutes) }}</span></div>
              <div class="detail-meta-item" v-if="(gameDetail?.rating || detailGame.vndbRating) && (gameDetail?.votecount || detailGame.vndbVotecount)"><el-icon color="#e6a23c"><StarFilled /></el-icon><span class="meta-label">投票</span><span class="meta-value">{{ gameDetail?.votecount || detailGame.vndbVotecount }} 票</span></div>
              <div class="detail-meta-item" v-if="(gameDetail?.platforms && gameDetail.platforms.length > 0) || (detailGame.vndbPlatforms && detailGame.vndbPlatforms.length > 0)"><el-icon color="#5b9bd5"><Monitor /></el-icon><span class="meta-label">平台</span><span class="meta-value">{{ (gameDetail?.platforms || detailGame.vndbPlatforms).join(', ') }}</span></div>
              <div class="detail-meta-item" v-if="(gameDetail?.languages && gameDetail.languages.length > 0) || (detailGame.vndbLanguages && detailGame.vndbLanguages.length > 0)"><el-icon color="#67c23a"><ChatDotRound /></el-icon><span class="meta-label">语言</span><span class="meta-value">{{ (gameDetail?.languages || detailGame.vndbLanguages).map(langName).join(', ') }}</span></div>
              <div class="detail-meta-item" v-if="gameDetail?.olang || detailGame.vndbOlang"><el-icon color="#002FA7"><Reading /></el-icon><span class="meta-label">原语</span><span class="meta-value">{{ langName(gameDetail?.olang || detailGame.vndbOlang || '') }}</span></div>
              <div class="detail-meta-item" v-if="detailGame.vndbId"><el-icon color="#002FA7"><Link /></el-icon><span class="meta-label">VNDB</span><a class="meta-value vndb-link" :href="'https://vndb.org/' + detailGame.vndbId" target="_blank">{{ detailGame.vndbId }}</a></div>
            </div>
            <div class="detail-categories" v-if="categories.length > 0">
              <span class="tag-label">分类</span>
              <span v-for="c in categories" :key="c.id" class="detail-tag category-chip" :class="{ 'is-on': (detailGame.categories || []).includes(c.name) }" :style="{ borderColor: c.color, backgroundColor: (detailGame.categories || []).includes(c.name) ? c.color + '20' : '' }" @click="toggleGameCategory(detailGame!, c)">{{ c.name }}</span>
            </div>
            <div class="detail-tags" v-if="(gameDetail?.tags && gameDetail.tags.length > 0) || (detailGame.tagsZh && detailGame.tagsZh.length > 0) || (detailGame.tags && detailGame.tags.length > 0)">
              <span class="tag-label">标签</span>
              <span v-for="tag in (gameDetail?.tags && gameDetail.tags.length > 0 ? gameDetail.tags : (detailGame.tagsZh && detailGame.tagsZh.length > 0 ? detailGame.tagsZh : detailGame.tags))" :key="tag" class="detail-tag">{{ tag }}</span>
            </div>
            <div class="detail-description" v-if="gameDetail?.descriptionZh || detailGame.descriptionZh || gameDetail?.description || detailGame.description">
              <h4 class="section-title">简介</h4>
              <p class="desc-text">{{ gameDetail?.descriptionZh || detailGame.descriptionZh || gameDetail?.description || detailGame.description }}</p>
            </div>
            <div class="detail-screenshots" v-if="detailGame.screenshots && detailGame.screenshots.length > 0">
              <h4 class="section-title">截图</h4>
              <div class="screenshots-grid">
                <div v-for="(ss, idx) in detailGame.screenshots" :key="'ss-'+idx" class="screenshot-item" @click="currentPreviewImg = ss; showImagePreview = true"><img :src="ss" :alt="'截图 '+(idx+1)" /></div>
              </div>
            </div>
            <el-dialog v-model="showImagePreview" width="auto" class="image-preview-dialog" :show-close="true" @click="showImagePreview=false"><img :src="currentPreviewImg" class="preview-image" /></el-dialog>
            <div class="detail-loading" v-if="detailLoading"><el-icon class="is-loading"><Loading /></el-icon><span>正在从 VNDB 获取信息...</span></div>
            <div class="detail-vndb-setup" v-if="!detailGame.vndbId && !detailLoading && !gameDetail">
              <div class="vndb-setup-hint" v-if="!vndbSearching && vndbSearchResults.length === 0 && !showVndbInput"><span>正在用本地游戏名搜索 VNDB...</span></div>
            </div>
          </div>
        </div>
        <template #footer><div class="custom-dialog-footer">
          <el-button round @click="showEditDialog = true; editingGame = JSON.parse(JSON.stringify(detailGame))">编辑信息</el-button>
          <el-button round @click="showDetailDialog=false">关闭</el-button>
          <el-button round type="primary" @click="launchGame(detailGame!)" color="#002FA7"><el-icon><VideoPlay /></el-icon> 启动游戏</el-button>
        </div></template>
      </el-dialog>

      <el-dialog v-model="showBatchVndbDialog" width="620px" class="custom-dialog" :show-close="false">
        <template #header><div class="custom-dialog-header"><span>VNDB 批量导入</span></div></template>
        <div v-if="batchVndbLoading" class="batch-vndb-loading"><el-icon class="is-loading" :size="32"><Loading /></el-icon><span>正在搜索 {{ batchVndbGameList.length }} 个游戏...</span></div>
        <div v-else-if="Object.keys(batchVndbResults).length > 0" class="batch-vndb-body">
          <div class="batch-vndb-progress"><span>为每个游戏选择匹配的 VNDB 条目 ({{ batchVndbCurrentIdx + 1 }} / {{ batchVndbGameList.length }})</span></div>
          <div class="batch-vndb-current" v-if="batchVndbCurrentGame">
            <div class="batch-vndb-game-name"><el-icon color="#002FA7"><VideoPlay /></el-icon><strong>{{ batchVndbCurrentGame.titleZh || batchVndbCurrentGame.title }}</strong></div>
            <div class="batch-vndb-results" v-if="batchVndbCurrentResults.length > 0">
              <div v-for="item in batchVndbCurrentResults" :key="item.id" class="batch-vndb-item" :class="{ 'is-selected': batchVndbSelections[batchVndbCurrentGame.id] === item.id }" @click="batchVndbSelections[batchVndbCurrentGame.id] = item.id">
                <img v-if="item.image?.thumbnail" :src="item.image.thumbnail" class="batch-vndb-thumb" />
                <div class="batch-vndb-info"><span class="batch-vndb-title">{{ item.title }}</span><span class="batch-vndb-meta" v-if="item.released || item.rating">{{ item.released }} <span v-if="item.rating">| 评分 {{ item.rating }}%</span></span></div>
                <el-icon v-if="batchVndbSelections[batchVndbCurrentGame.id] === item.id" color="#002FA7" :size="20"><CircleCheckFilled /></el-icon>
              </div>
            </div>
            <div class="batch-vndb-no-result" v-else><span>未找到匹配，可跳过或手动输入 VNDB ID</span><el-input v-model="batchVndbManualId" placeholder="输入 VNDB ID" size="small" style="width:140px;" @keyup.enter="skipVndbCurrent" /></div>
          </div>
          <div class="batch-vndb-footer-btns">
            <el-button size="small" @click="batchVndbCurrentIdx--" :disabled="batchVndbCurrentIdx === 0">上一个</el-button>
            <el-button size="small" @click="skipVndbCurrent">跳过</el-button>
            <el-button size="small" type="primary" @click="nextVndbCurrent" color="#002FA7" :disabled="batchVndbCurrentIdx >= batchVndbGameList.length - 1 && !batchVndbSelections[batchVndbCurrentGame?.id || '']">{{ batchVndbCurrentIdx >= batchVndbGameList.length - 1 ? '完成' : '下一个' }}</el-button>
          </div>
        </div>
        <template #footer><div class="custom-dialog-footer">
          <el-button round @click="showBatchVndbDialog=false">关闭</el-button>
          <el-button round type="primary" @click="confirmBatchVndb" :disabled="Object.values(batchVndbSelections).filter(Boolean).length === 0" :loading="batchVndbSaving" color="#002FA7">确认导入 ({{ Object.values(batchVndbSelections).filter(Boolean).length }})</el-button>
        </div></template>
      </el-dialog>

      <el-dialog v-model="showBatchResultDialog" width="520px" class="custom-dialog batch-result-dialog" :show-close="true" @closed="batchResultData = null">
        <template #header><div class="custom-dialog-header" :class="{ 'header-success': batchResultData && batchResultData.failed === 0, 'header-warn': batchResultData && batchResultData.failed > 0 }"><span>批量导入完成</span></div></template>
        <div class="batch-result-body" v-if="batchResultData">
          <div class="result-summary">
            <div class="result-stat success-stat"><el-icon :size="28" color="#67c23a"><CircleCheckFilled /></el-icon><div class="stat-text"><span class="stat-num">{{ batchResultData.enriched }}</span><span class="stat-label">导入成功</span></div></div>
            <div class="result-stat fail-stat" v-if="batchResultData.failed > 0"><el-icon :size="28" color="#f56c6c"><CircleCloseFilled /></el-icon><div class="stat-text"><span class="stat-num">{{ batchResultData.failed }}</span><span class="stat-label">导入失败</span></div></div>
          </div>
          <div class="result-detail-list" v-if="batchResultData.failed > 0">
            <h4 class="result-detail-title">失败详情</h4>
            <div v-for="r in batchResultData.results.filter(x => !x.success)" :key="r.gameId || r.name" class="result-detail-item">
              <el-icon color="#f56c6c" :size="14"><WarningFilled /></el-icon><span class="result-detail-name">{{ r.name || r.gameId || '未知游戏' }}</span><span class="result-detail-err">{{ r.error }}</span>
            </div>
          </div>
        </div>
        <template #footer><div class="custom-dialog-footer"><el-button round type="primary" @click="showBatchResultDialog=false" color="#002FA7">知道了</el-button></div></template>
      </el-dialog>

      <el-dialog v-model="showEditDialog" width="480px" class="custom-dialog" :show-close="false">
        <template #header><div class="custom-dialog-header"><span>编辑游戏信息</span></div></template>
        <div class="settings-list" v-if="editingGame">
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#002FA7"><EditPen /></el-icon><span>原名</span></div>
            <div class="settings-item-control"><el-input v-model="editingGame.title" size="small" style="width:200px;" /></div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#002FA7"><EditPen /></el-icon><span>中文译名</span></div>
            <div class="settings-item-control"><el-input v-model="editingGame.titleZh" size="small" style="width:200px;" /></div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#4a7fd4"><Link /></el-icon><span>启动程序</span></div>
            <div class="settings-item-control" style="gap:8px;">
              <el-input v-model="editingGame.exePath" placeholder="点击选择..." size="small" style="width:160px;" readonly />
              <el-button size="small" class="pick-btn" @click="pickFileEditing('exePath','.exe')"><el-icon><Folder /></el-icon></el-button>
            </div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#5b9bd5"><Location /></el-icon><span>游戏目录</span></div>
            <div class="settings-item-control" style="gap:8px;">
              <el-input v-model="editingGame.gameDir" placeholder="点击选择..." size="small" style="width:160px;" readonly />
              <el-button size="small" class="pick-btn" @click="pickDirectoryEditing('gameDir')"><el-icon><Folder /></el-icon></el-button>
            </div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#67c23a"><Switch /></el-icon><span>转区启动</span></div>
            <div class="settings-item-control"><el-switch v-model="editingGame.needLocaleEmulator" style="--el-switch-on-color:#002FA7" /></div>
          </div>
          <div class="settings-item-row">
            <div class="settings-item-label"><el-icon color="#5b8def"><Hide /></el-icon><span>模糊封面</span></div>
            <div class="settings-item-control"><el-switch v-model="editingGame.blurred" style="--el-switch-on-color:#002FA7" /></div>
          </div>
        </div>
        <template #footer><div class="custom-dialog-footer">
          <el-button round type="danger" @click="deleteGame">删除游戏</el-button>
          <el-button round @click="showEditDialog=false">取消</el-button>
          <el-button round type="primary" @click="saveGameEdit" color="#002FA7">保存</el-button>
        </div></template>
      </el-dialog>
    </el-container>
  </el-config-provider>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

interface Game {
  id: string; title: string; titleZh: string; gameDir: string; exePath: string;
  coverUrl: string; tags: string[]; tagsZh: string[]; screenshots: string[];
  developer: string; vndbId: string; vndbTitle: string; vndbAlttitle: string;
  description: string; descriptionZh: string; vndbRating: number; vndbVotecount: number;
  vndbReleased: string; vndbLength: number; vndbLengthMinutes: number;
  vndbPlatforms: string[]; vndbOlang: string; vndbLanguages: string[];
  needLocaleEmulator: boolean; blurred: boolean; lastPlayTime: number;
  favorite: boolean; categories: string[]; sortOrder: number;
}

interface GameInfoResult {
  vndbId: string; title: string; alttitle: string; description: string;
  descriptionZh: string; developer: string; developerOriginal: string;
  tags: string[]; tagsZh: string[]; released: string; rating: number;
  votecount: number; length: number; lengthMinutes: number; coverUrl: string;
  screenshotUrls: string[]; platforms: string[]; olang: string; languages: string[];
}

interface VndbSearchItem {
  id: string; title: string; alttitle: string; released: string; rating: number;
  image?: { thumbnail: string }; developer?: string;
}

interface EnrichItemResult { gameId: string; name: string; success: boolean; error: string; }

interface BatchEnrichResult { total: number; enriched: number; failed: number; results: EnrichItemResult[]; }

interface Category { id: string; name: string; color: string; sortOrder: number; createdTime: number; gameCount: number; }

const games = ref<Game[]>([])
const searchQuery = ref('')
const defaultCover = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9IiNlMGUwZTAiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZmlsbD0iIzhmOGY4ZiIgZm9udC1zaXplPSIyMCIgdGV4dC1hbmNob3I9Im1pZGRsZSI+Tm8gQ292ZXI8L3RleHQ+PC9zdmc+'

const showScanDialog = ref(false)
const scanning = ref(false)
const scanForm = ref({ path: '', depth: 3, fuzzy: false })

const showConfigDialog = ref(false)
const configForm = ref({ leProcPath: '', languagePreference: 'zh', autoScanEnabled: false, autoScanPath: '', globalBlur: false, deepseekApiKey: '', serverAddress: '0.0.0.0', serverPort: 8080 })

const showAddDialog = ref(false)
const adding = ref(false)
const addForm = ref({ title: '', exePath: '', gameDir: '', needLocaleEmulator: false })

const showEditDialog = ref(false)
const editingGame = ref<Game | null>(null)

const showDetailDialog = ref(false)
const detailGame = ref<Game | null>(null)
const gameDetail = ref<GameInfoResult | null>(null)
const detailLoading = ref(false)
const vndbSearching = ref(false)
const vndbSearchResults = ref<VndbSearchItem[]>([])
const vndbSearched = ref(false)
const showVndbInput = ref(false)

const selectMode = ref(false)
const selectedIds = ref(new Set<string>())
const batchVndbLoading = ref(false)
const batchDeleting = ref(false)
const batchVndbSaving = ref(false)
const batchVndbResults = ref<Record<string, VndbSearchItem[]>>({})
const batchVndbSelections = ref<Record<string, string>>({})
const batchVndbCurrentIdx = ref(0)
const batchVndbManualId = ref('')
const showBatchVndbDialog = ref(false)

const showImagePreview = ref(false)
const currentPreviewImg = ref('')

const showAddCategoryDialog = ref(false)
const newCategoryName = ref('')
const newCategoryColor = ref('#5b8def')
const categories = ref<Category[]>([])

const showBatchResultDialog = ref(false)
const batchResultData = ref<BatchEnrichResult | null>(null)

const dragId = ref<string | null>(null)
const restarting = ref(false)

const activeFilter = ref<{ type: string; value: string }>({ type: 'all', value: '' })

const fetchGames = async () => {
  try {
    const res = await axios.get(`/api/games`)
    games.value = res.data
  } catch (e) { ElMessage.error('获取游戏列表失败') }
}

const fetchConfig = async () => {
  try {
    const res = await axios.get('/api/config')
    configForm.value = res.data
  } catch (e) { console.error(e) }
}

const fetchCategories = async () => {
  try {
    const res = await axios.get('/api/categories')
    categories.value = res.data
  } catch (e) { console.error('获取分类列表失败', e) }
}

const startScan = async () => {
  if (!scanForm.value.path) { ElMessage.warning('请输入要扫描的路径'); return }
  scanning.value = true
  try {
    const res = await axios.post('/api/games/scan', scanForm.value)
    ElMessage.success(`扫描完成，共找到 ${res.data.count} 个新游戏`)
    showScanDialog.value = false; fetchGames()
  } catch (e) { ElMessage.error('扫描失败') } finally { scanning.value = false }
}

const saveConfig = async () => {
  try {
    await axios.put('/api/config', configForm.value)
    ElMessage.success('设置已保存'); showConfigDialog.value = false
  } catch (e) { ElMessage.error('保存设置失败') }
}

const restartServer = async () => {
  if (!confirm('修改地址或端口后需要重启服务才能生效。\n\n确定要保存设置并重启服务吗？\n重启期间服务将短暂不可用，约 2-3 秒后恢复。')) return
  restarting.value = true
  try {
    await axios.put('/api/config', configForm.value)
    await axios.post('/api/system/restart')
    ElMessage.success('服务正在重启，3 秒后自动刷新...')
    showConfigDialog.value = false
    setTimeout(() => { window.location.reload() }, 3000)
  } catch (e) { ElMessage.error('重启失败'); restarting.value = false }
}

const shutdownServer = async () => {
  if (!confirm('确定要退出 GalSpace 程序吗？\n\n退出后服务将停止，浏览器将无法访问。')) return
  try {
    await axios.post('/api/system/shutdown')
    ElMessage.success('服务已关闭')
  } catch (e) {
    // 服务关闭后请求可能抛出错误，忽略即可
    ElMessage.success('服务已关闭')
  }
}

const manualAddGame = async () => {
  if (!addForm.value.title || !addForm.value.exePath || !addForm.value.gameDir) { ElMessage.warning('请填写完整必填信息'); return }
  adding.value = true
  try {
    await axios.post('/api/games', { ...addForm.value, titleZh: addForm.value.title, tags: [] })
    ElMessage.success('手动添加游戏成功'); showAddDialog.value = false
    addForm.value = { title: '', exePath: '', gameDir: '', needLocaleEmulator: false }; fetchGames()
  } catch (e) { ElMessage.error('手动添加游戏失败') } finally { adding.value = false }
}

const launchGame = async (game: Game) => {
  try {
    ElMessage.info(`正在启动 ${game.titleZh || game.title}...`)
    await axios.post(`/api/games/${game.id}/launch`)
  } catch (e) { ElMessage.error('启动失败，请检查路径配置') }
}

const openGameDetails = (game: Game) => {
  detailGame.value = game
  gameDetail.value = null
  detailLoading.value = false
  vndbSearchResults.value = []
  vndbSearched.value = false
  showVndbInput.value = false
  showDetailDialog.value = true
  if (game.vndbId) {
    detailLoading.value = true
    axios.get(`/api/games/${game.id}/vndb-info`).then(res => {
      gameDetail.value = res.data; detailLoading.value = false
    }).catch(() => { detailLoading.value = false })
  }
}

const saveGameEdit = async () => {
  if (!editingGame.value) return
  try {
    await axios.put(`/api/games/${editingGame.value.id}`, editingGame.value)
    ElMessage.success('保存成功'); showEditDialog.value = false; fetchGames()
  } catch (e) { ElMessage.error('保存失败') }
}

const deleteGame = async () => {
  if (!editingGame.value) return
  if (confirm('确定要从库中移除此游戏吗？（不会删除本地文件）')) {
    try {
      await axios.delete(`/api/games/${editingGame.value.id}`)
      ElMessage.success('删除成功'); showEditDialog.value = false; fetchGames()
    } catch (e) { ElMessage.error('删除失败') }
  }
}

const openSystemDialog = async (isDirectory: boolean, currentPath?: string, extension?: string): Promise<string> => {
  try {
    const params: any = { isDirectory }
    if (extension) params.extension = extension
    if (currentPath) params.currentPath = currentPath
    const res = await axios.get('/api/system/dialog/file', { params })
    return res.data.path || ''
  } catch (e) { ElMessage.error('打开文件选择框失败'); return '' }
}

const getCurrentPath = (formName: string, fieldName: string): string => {
  if (formName === 'configForm') return (configForm.value as any)[fieldName] || ''
  if (formName === 'scanForm') return (scanForm.value as any)[fieldName] || ''
  if (formName === 'addForm') return (addForm.value as any)[fieldName] || ''
  return ''
}

const pickFile = async (formName: string, fieldName: string, extension?: string) => {
  const path = await openSystemDialog(false, getCurrentPath(formName, fieldName), extension)
  if (path) {
    if (formName === 'configForm') (configForm.value as any)[fieldName] = path
    if (formName === 'addForm') { (addForm.value as any)[fieldName] = path; if (fieldName === 'exePath' && !addForm.value.gameDir) { const i = path.lastIndexOf('\\'); if (i > 0) addForm.value.gameDir = path.substring(0, i) } }
  }
}

const pickDirectory = async (formName: string, fieldName: string) => {
  const path = await openSystemDialog(true, getCurrentPath(formName, fieldName))
  if (path) {
    if (formName === 'configForm') (configForm.value as any)[fieldName] = path
    if (formName === 'scanForm') (scanForm.value as any)[fieldName] = path
    if (formName === 'addForm') (addForm.value as any)[fieldName] = path
  }
}

const pickFileEditing = async (fieldName: string, extension?: string) => {
  const cp = editingGame.value ? (editingGame.value as any)[fieldName] || '' : ''
  const path = await openSystemDialog(false, cp, extension)
  if (path && editingGame.value) {
    (editingGame.value as any)[fieldName] = path
    if (fieldName === 'exePath') { const i = path.lastIndexOf('\\'); if (i > 0) editingGame.value.gameDir = path.substring(0, i) }
  }
}

const pickDirectoryEditing = async (fieldName: string) => {
  const cp = editingGame.value ? (editingGame.value as any)[fieldName] || '' : ''
  const path = await openSystemDialog(true, cp)
  if (path && editingGame.value) (editingGame.value as any)[fieldName] = path
}

const filteredGames = computed(() => {
  let result = games.value
  const q = searchQuery.value.trim().toLowerCase()
  if (q) result = result.filter(g => (g.title && g.title.toLowerCase().includes(q)) || (g.titleZh && g.titleZh.toLowerCase().includes(q)) || (g.tags && g.tags.some(t => t.toLowerCase().includes(q))))
  if (activeFilter.value.type === 'tag') result = result.filter(g => (g.tags && g.tags.some(t => t.trim() === activeFilter.value.value)) || (g.tagsZh && g.tagsZh.some(t => t.trim() === activeFilter.value.value)))
  else if (activeFilter.value.type === 'dev') result = result.filter(g => g.developer?.trim() === activeFilter.value.value)
  else if (activeFilter.value.type === 'favorite') result = result.filter(g => g.favorite)
  else if (activeFilter.value.type === 'category') result = result.filter(g => g.categories && g.categories.includes(activeFilter.value.value))
  return result
})

const favoriteCount = computed(() => games.value.filter(g => g.favorite).length)

const allTags = computed(() => {
  const tagMap = new Map<string, string>() // zh -> en
  games.value.forEach(g => {
    if (g.tagsZh && g.tagsZh.length > 0 && g.tags && g.tags.length > 0) {
      for (let i = 0; i < Math.min(g.tagsZh.length, g.tags.length); i++) {
        const zh = g.tagsZh[i].trim()
        const en = g.tags[i].trim()
        if (zh && en && !tagMap.has(zh)) tagMap.set(zh, en)
      }
    } else if (g.tags) {
      g.tags.forEach(t => { const tt = t.trim(); if (tt && !tagMap.has(tt)) tagMap.set(tt, tt) })
    }
  })
  return Array.from(tagMap.entries()).sort((a,b) => a[0].localeCompare(b[0]))
})
const allDevs = computed(() => { const s = new Set<string>(); games.value.forEach(g => { if (g.developer) s.add(g.developer.trim()) }); return Array.from(s).sort() })

const setFilter = (type: string, value: string = '') => { activeFilter.value = { type, value } }

const toggleSelectMode = () => { selectMode.value = !selectMode.value; if (!selectMode.value) selectedIds.value = new Set() }
const toggleSelect = (id: string) => { const n = new Set(selectedIds.value); if (n.has(id)) n.delete(id); else n.add(id); selectedIds.value = n }
const selectAll = () => { selectedIds.value = new Set(filteredGames.value.map(g => g.id)) }
const clearSelection = () => { selectedIds.value = new Set() }

const toggleFavorite = async (game: Game) => {
  const ug = { ...game, favorite: !game.favorite }
  try { await axios.put(`/api/games/${game.id}`, ug); const i = games.value.findIndex(g => g.id === game.id); if (i >= 0) games.value[i] = ug } catch (e) { ElMessage.error('操作失败') }
}

const createCategory = async () => {
  const name = newCategoryName.value.trim()
  if (!name) { ElMessage.warning('请输入分类名称'); return }
  try {
    await axios.post('/api/categories', { name, color: newCategoryColor.value })
    ElMessage.success(`分类「${name}」创建成功`); newCategoryName.value = ''; newCategoryColor.value = '#5b8def'; showAddCategoryDialog.value = false; await fetchCategories(); setFilter('category', name)
  } catch (e: any) {
    if (e.response?.status === 409) { ElMessage.warning(e.response.data.message || '分类已存在'); await fetchCategories(); showAddCategoryDialog.value = false; setFilter('category', name) }
    else { ElMessage.error('创建分类失败') }
  }
}

const deleteCategoryConfirm = (cat: Category) => {
  if (!confirm(`确定要删除分类「${cat.name}」吗？\n该分类下的 ${cat.gameCount} 款游戏将取消关联，但不会被删除。`)) return
  deleteCategory(cat)
}

const deleteCategory = async (cat: Category) => {
  try {
    await axios.delete(`/api/categories/${cat.id}`)
    ElMessage.success(`分类「${cat.name}」已删除`); await fetchCategories()
    if (activeFilter.value.type === 'category' && activeFilter.value.value === cat.name) setFilter('all')
    fetchGames()
  } catch (e) { ElMessage.error('删除分类失败') }
}

const toggleGameCategory = async (game: Game, cat: Category) => {
  const has = (game.categories || []).includes(cat.name)
  try {
    if (has) await axios.delete(`/api/categories/${cat.id}/games/${game.id}`)
    else await axios.post(`/api/categories/${cat.id}/games/${game.id}`)
    const nc = has ? (game.categories || []).filter(c => c !== cat.name) : [...(game.categories || []), cat.name]
    game.categories = nc; detailGame.value = { ...game }
    const i = games.value.findIndex(g => g.id === game.id); if (i >= 0) games.value[i] = { ...game }
    await fetchCategories()
  } catch (e) { ElMessage.error('操作失败') }
}

const onDragStart = (e: DragEvent, id: string) => { if (selectMode.value) return; dragId.value = id; if (e.dataTransfer) { e.dataTransfer.effectAllowed = 'move'; e.dataTransfer.setData('text/plain', id) } }
const onDragOver = (e: DragEvent, _id: string) => { if (dragId.value && e.dataTransfer) e.dataTransfer.dropEffect = 'move' }
const onDragLeave = (_e: DragEvent) => {}

const onDrop = (e: DragEvent, targetId: string) => {
  if (!dragId.value || dragId.value === targetId) { dragId.value = null; return }
  const si = games.value.findIndex(g => g.id === dragId.value); const di = games.value.findIndex(g => g.id === targetId)
  if (si < 0 || di < 0) { dragId.value = null; return }
  const u = [...games.value]; const [m] = u.splice(si, 1); u.splice(di, 0, m); games.value = u; dragId.value = null
  saveGameOrder()
}

const saveGameOrder = async () => { try { await axios.put('/api/games/reorder', games.value.map(g => g.id)) } catch (e) { console.error('保存排序失败', e) } }

const batchDelete = async () => {
  if (selectedIds.value.size === 0) return
  if (!confirm(`确定要删除选中的 ${selectedIds.value.size} 款游戏吗？（不会删除本地文件）`)) return
  batchDeleting.value = true
  try { await axios.post('/api/games/batch/delete', Array.from(selectedIds.value)); ElMessage.success('批量删除完成'); selectedIds.value = new Set(); selectMode.value = false; fetchGames() }
  catch (e) { ElMessage.error('批量删除失败') } finally { batchDeleting.value = false }
}

const batchVndbFetch = async () => {
  batchVndbLoading.value = true; showBatchVndbDialog.value = true
  batchVndbResults.value = {}; batchVndbSelections.value = {}; batchVndbCurrentIdx.value = 0
  try {
    const ids = Array.from(selectedIds.value)
    const res = await axios.post('/api/games/batch/vndb-search', ids)
    batchVndbResults.value = res.data
  } catch (e) { ElMessage.error('VNDB 搜索失败') } finally { batchVndbLoading.value = false }
}

const batchVndbGameList = computed(() => {
  const ids = Object.keys(batchVndbResults.value)
  return ids.map(id => games.value.find(g => g.id === id)).filter(Boolean) as Game[]
})

const batchVndbCurrentGame = computed(() => batchVndbGameList.value[batchVndbCurrentIdx.value] || null)
const batchVndbCurrentResults = computed(() => { if (!batchVndbCurrentGame.value) return []; return batchVndbResults.value[batchVndbCurrentGame.value.id] || [] })

const skipVndbCurrent = () => {
  if (batchVndbManualId.value.trim()) {
    batchVndbSelections.value[batchVndbCurrentGame.value!.id] = batchVndbManualId.value.trim()
  }
  batchVndbManualId.value = ''; batchVndbCurrentIdx.value++
}

const nextVndbCurrent = () => { skipVndbCurrent() }

const confirmBatchVndb = async () => {
  const items = Object.entries(batchVndbSelections.value).filter(([,v]) => v && v.trim()).map(([gameId, vndbId]) => ({ gameId, vndbId }))
  if (items.length === 0) { ElMessage.warning('请至少选择一个匹配项'); return }
  batchVndbSaving.value = true
  try {
    const res = await axios.post('/api/games/batch/vndb-enrich', items)
    const result: BatchEnrichResult = res.data
    showBatchVndbDialog.value = false; batchVndbResults.value = {}; batchVndbSelections.value = {}; batchVndbCurrentIdx.value = 0
    selectedIds.value = new Set(); selectMode.value = false
    await fetchGames()
    batchResultData.value = result; showBatchResultDialog.value = true
  } catch (e) { ElMessage.error('批量 VNDB 导入失败') } finally { batchVndbSaving.value = false }
}

const batchAddToCategory = async (categoryId: string) => {
  if (selectedIds.value.size === 0) return
  try {
    await axios.post(`/api/categories/${categoryId}/games/batch`, { gameIds: Array.from(selectedIds.value) })
    ElMessage.success(`已将 ${selectedIds.value.size} 款游戏添加到分类`); await fetchCategories(); selectedIds.value = new Set(); selectMode.value = false; fetchGames()
  } catch (e) { ElMessage.error('批量添加到分类失败') }
}

const formatMinutes = (m: number): string => { if (!m) return ''; if (m < 60) return `${m}分钟`; const h = Math.floor(m/60); const r = m%60; return r>0?`${h}小时${r}分钟`:`${h}小时` }

const langName = (code: string): string => {
  const map: Record<string,string> = { ja:'日语',en:'英语',zh:'中文','zh-Hans':'简中','zh-Hant':'繁中',ko:'韩语',fr:'法语',de:'德语',ru:'俄语',es:'西班牙语',pt:'葡萄牙语',it:'意大利语',vi:'越南语',th:'泰语',ar:'阿拉伯语',pl:'波兰语',cs:'捷克语',sv:'瑞典语',nl:'荷兰语',fi:'芬兰语',hu:'匈牙利语',tr:'土耳其语',uk:'乌克兰语',id:'印尼语' }
  return map[code] || code
}

onMounted(() => { fetchGames(); fetchConfig(); fetchCategories() })
</script>

<style>
.custom-dialog { border-radius: 20px !important; overflow: hidden; box-shadow: 0 20px 50px rgba(0,0,0,0.15) !important; }
.custom-dialog .el-dialog__header { padding: 0 !important; margin: 0 !important; }
.custom-dialog .el-dialog__body { padding: 20px 30px !important; background-color: #eef2f7; }
.custom-dialog .el-dialog__footer { padding: 15px 30px 20px !important; background-color: #eef2f7; border-top: none; }
</style>

<style scoped>
.app-container { height: 100vh; display: flex; background-color: #e8edf5; background-image: radial-gradient(ellipse at 25% 8%, #d4e0f0 0%, transparent 55%), radial-gradient(ellipse at 75% 92%, #c8d6ea 0%, transparent 55%); font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
.sidebar { background-color: #ffffff; display: flex; flex-direction: column; padding: 20px 0; box-shadow: 2px 0 10px rgba(0,0,0,0.02); z-index: 10; }
.logo { font-size: 22px; font-weight: bold; color: #002FA7; padding: 0 20px; margin-bottom: 30px; letter-spacing: 1px; }
.avatar-container { display: flex; justify-content: center; margin-bottom: 30px; }
.avatar-placeholder { width: 120px; height: 120px; background-color: #e4ebf5; border-radius: 20px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 15px rgba(0,47,167,0.08); }
.sidebar-menu { border-right: none; flex: 1; padding: 0 10px; }
.el-menu-item, .el-sub-menu__title { height: 44px; line-height: 44px; border-radius: 8px; margin-bottom: 4px; color: #666; }
.el-menu-item.is-active { background-color: #e8edf5; color: #002FA7; font-weight: bold; }
.menu-title { font-size: 14px; }
.badge { float: right; font-size: 12px; color: #aaa; }
.add-category { color: #aaa !important; }

.bottom-menu { padding: 0 20px; display: flex; flex-direction: column; gap: 15px; }
.menu-item { display: flex; align-items: center; gap: 10px; color: #666; font-size: 14px; cursor: pointer; border-radius: 8px; padding: 6px 10px; transition: background-color 0.15s; }
.menu-item:hover { background-color: #f0f3fa; }
.menu-item.is-active { background-color: #e8edf5; color: #002FA7; font-weight: 600; }
.menu-item .badge { margin-left: auto; }
.settings-item { margin-top: 10px; padding: 10px 15px; border: 1px solid #5b8def; border-radius: 20px; color: #333; justify-content: center; transition: background-color 0.2s; }
.settings-item:hover { background-color: #e4ebf5; }

.exit-item { margin-top: 4px; border-color: #f56c6c !important; color: #f56c6c !important; }
.exit-item:hover { background-color: #fef0f0 !important; }

.category-row .category-delete-icon { opacity: 0; transition: opacity 0.15s; color: #999; flex-shrink: 0; }
.category-row:hover .category-delete-icon { opacity: 1; }
.category-row .category-delete-icon:hover { color: #f56c6c; }

.main-container { flex: 1; display: flex; flex-direction: column; overflow: hidden; padding: 20px 30px; }
.main-header { padding: 0; margin-bottom: 20px; }
.header-inner { background-color: #ffffff; border-radius: 16px; height: 100%; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.03); }
.stats-bar { display: flex; gap: 25px; color: #666; font-size: 14px; }
.stats-bar strong { font-size: 18px; color: #333; margin-right: 4px; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.search-input { width: 200px; }
.search-input :deep(.el-input__wrapper) { border-radius: 20px; background-color: #f5f7fa; box-shadow: none; }
.search-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #002FA7 inset; }
.action-btn { border-radius: 20px; border: 1px solid #ebeef5; color: #606266; }
.action-btn:hover { color: #002FA7; border-color: #5b8def; background-color: #e8edf5; }
.is-active-btn { color: #002FA7; background-color: #e8edf5; border-color: #002FA7; }
.batch-bar { background: #fff; border-radius: 12px; padding: 12px 20px; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.batch-count { font-size: 14px; color: #666; }
.batch-actions { display: flex; gap: 8px; }
.main-content { padding: 0; overflow-y: auto; }
.game-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 24px; padding-bottom: 40px; }
.game-card { position: relative; background: #ffffff; border-radius: 16px; overflow: hidden; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; box-shadow: 0 4px 12px rgba(0,47,167,0.04); }
.game-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,47,167,0.1); }
.game-card:active { cursor: grabbing; }
.game-card.drag-over { outline: 2px dashed #002FA7; outline-offset: -4px; border-radius: 16px; }
.game-card.is-selected { outline: 2px solid #002FA7; outline-offset: -3px; }
.select-check { position: absolute; top: 8px; right: 8px; z-index: 5; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.9); border-radius: 50%; box-shadow: 0 2px 6px rgba(0,0,0,0.12); cursor: pointer; }
.card-favorite { position: absolute; top: 8px; left: 8px; z-index: 5; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.85); border-radius: 50%; box-shadow: 0 2px 6px rgba(0,0,0,0.12); cursor: pointer; transition: transform 0.15s; }
.card-favorite:hover { transform: scale(1.2); }
.cover-wrapper { position: relative; width: 100%; aspect-ratio: 3/4; overflow: hidden; background-color: #f9f9f9; padding: 10px 10px 0 10px; }
.game-number { position: absolute; top: 18px; left: 18px; background-color: rgba(0,47,167,0.75); color: white; font-size: 10px; padding: 2px 6px; border-radius: 4px; z-index: 2; font-weight: bold; }
.cover-image { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; transition: filter 0.3s; }
.blurred-cover { filter: blur(15px); }
.cover-wrapper:hover .blurred-cover { filter: blur(5px); }
.play-overlay { position: absolute; top: 10px; left: 10px; right: 10px; bottom: 0; border-radius: 8px; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s; z-index: 3; }
.cover-wrapper:hover .play-overlay { opacity: 1; }
.play-icon { font-size: 48px; color: white; }
.game-info { padding: 12px 16px; }
.game-title { margin: 0 0 6px 0; font-size: 14px; font-weight: bold; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tags { display: flex; gap: 6px; flex-wrap: wrap; }
.custom-tag { font-size: 10px; color: #999; background-color: #f5f5f5; padding: 2px 6px; border-radius: 4px; }
.fab-button { position: fixed; right: 40px; bottom: 40px; width: 56px; height: 56px; background-color: #002FA7; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 24px; cursor: pointer; box-shadow: 0 4px 15px rgba(0,47,167,0.4); transition: transform 0.2s; z-index: 100; }
.fab-button:hover { transform: scale(1.1); }
.custom-dialog-header { background: linear-gradient(135deg, #001a5e 0%, #002FA7 50%, #3b6fd4 100%); color: white; text-align: center; padding: 20px 0; font-size: 18px; font-weight: bold; letter-spacing: 2px; }
.settings-list { display: flex; flex-direction: column; gap: 12px; }
.settings-item-row { display: flex; justify-content: space-between; align-items: center; background-color: #ffffff; padding: 12px 20px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,47,167,0.05); transition: background-color 0.2s; }
.settings-item-row:hover { background-color: #f0f4fc; }
.settings-section-divider { text-align: center; padding: 8px 0 4px; font-size: 11px; color: #aaa; letter-spacing: 1px; }
.settings-item-label { display: flex; align-items: center; gap: 10px; color: #444; font-size: 14px; font-weight: 500; }
.settings-item-control { display: flex; align-items: center; }
.pick-btn { width: 34px; height: 34px; padding: 0; border-radius: 8px; border: 1px solid #dcdfe6; color: #606266; display: flex; align-items: center; justify-content: center; }
.pick-btn:hover { color: #002FA7; border-color: #002FA7; }
.custom-dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }
.setting-hint { font-size: 11px; color: #aaa; margin-left: 6px; }

.detail-dialog :deep(.el-dialog__body) { background-color: #f0f2f5; padding: 32px 44px !important; max-height: calc(88vh - 80px); overflow-y: auto; }
.detail-container { display: flex; gap: 48px; min-height: 480px; }
.detail-cover-section { flex-shrink: 0; width: 340px; display: flex; flex-direction: column; align-items: center; }
.detail-cover { width: 320px; height: 430px; object-fit: cover; border-radius: 16px; box-shadow: 0 12px 36px rgba(0,0,0,0.2); transition: filter 0.3s; }
.detail-cover.blurred-cover { filter: blur(12px); }
.detail-rating { margin-top: 16px; display: flex; align-items: baseline; gap: 4px; }
.rating-score { font-size: 38px; font-weight: bold; color: #002FA7; }
.rating-unit { font-size: 16px; color: #999; }
.detail-info-section { flex: 1; display: flex; flex-direction: column; gap: 20px; min-width: 0; }
.detail-title-row { margin-bottom: 0; }
.detail-title { font-size: 28px; font-weight: bold; color: #1a1a1a; margin: 0; line-height: 1.4; }
.detail-title-orig { font-size: 15px; color: #888; margin: 8px 0 0 0; }
.detail-title-sub { font-size: 14px; color: #aaa; margin: 4px 0 0 0; font-style: italic; }
.detail-meta { display: flex; flex-wrap: wrap; gap: 10px 28px; }
.detail-meta-item { display: flex; align-items: center; gap: 8px; font-size: 15px; color: #555; }
.meta-label { color: #999; font-size: 12px; }
.meta-value { color: #333; font-weight: 500; }
.meta-original { color: #aaa; font-size: 12px; }
.vndb-link { color: #002FA7; text-decoration: none; }
.vndb-link:hover { text-decoration: underline; }
.detail-tags { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tag-label { font-size: 12px; color: #999; }
.detail-tag { font-size: 11px; color: #5b8def; background-color: #e8edf5; padding: 3px 10px; border-radius: 12px; }
.detail-categories { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.category-chip { cursor: pointer; border: 1.5px solid #ddd; background-color: transparent !important; color: #888 !important; transition: all 0.15s; }
.category-chip.is-on { color: inherit !important; font-weight: 500; }
.category-chip:hover { opacity: 0.8; }
.detail-description { background-color: #ffffff; border-radius: 10px; padding: 14px 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.section-title { font-size: 14px; font-weight: 600; color: #666; margin: 0 0 10px 0; }
.desc-text { font-size: 15px; color: #555; line-height: 2.0; margin: 0; max-height: 340px; overflow-y: auto; }
.detail-loading { display: flex; align-items: center; gap: 8px; color: #999; font-size: 13px; padding: 12px 0; }
.detail-vndb-setup { padding: 8px 0; }
.vndb-setup-hint { display: flex; align-items: center; }
.detail-screenshots { margin-top: 4px; }
.screenshots-grid { display: flex; gap: 12px; overflow-x: auto; padding-bottom: 4px; }
.screenshots-grid::-webkit-scrollbar { height: 4px; }
.screenshots-grid::-webkit-scrollbar-thumb { background: #d0d5e0; border-radius: 2px; }
.screenshot-item { flex-shrink: 0; width: 230px; height: 130px; border-radius: 10px; overflow: hidden; cursor: pointer; background: #e0e0e0; transition: transform 0.15s; }
.screenshot-item:hover { transform: scale(1.03); }
.screenshot-item img { width: 100%; height: 100%; object-fit: cover; }
.image-preview-dialog :deep(.el-dialog__body) { padding: 0 !important; display: flex; align-items: center; justify-content: center; }
.image-preview-dialog :deep(.el-dialog__header) { padding: 0; margin: 0; }
.preview-image { max-width: 85vw; max-height: 85vh; border-radius: 8px; }

.batch-vndb-loading { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 40px; color: #999; }
.batch-vndb-progress { font-size: 13px; color: #888; margin-bottom: 12px; }
.batch-vndb-game-name { display: flex; align-items: center; gap: 8px; font-size: 15px; margin-bottom: 12px; }
.batch-vndb-item { display: flex; align-items: center; gap: 12px; padding: 10px; border: 1px solid #eee; border-radius: 8px; cursor: pointer; transition: all 0.15s; }
.batch-vndb-item.is-selected { border-color: #002FA7; background: #e8edf5; }
.batch-vndb-thumb { width: 48px; height: 64px; object-fit: cover; border-radius: 4px; flex-shrink: 0; }
.batch-vndb-info { flex: 1; min-width: 0; }
.batch-vndb-title { font-size: 13px; font-weight: 500; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.batch-vndb-meta { font-size: 11px; color: #999; }
.batch-vndb-no-result { display: flex; align-items: center; gap: 12px; color: #999; font-size: 13px; padding: 16px 0; }
.batch-vndb-footer-btns { display: flex; gap: 8px; margin-top: 16px; }

.header-success { background: linear-gradient(135deg, #4caf50 0%, #66bb6a 50%, #81c784 100%) !important; }
.header-warn { background: linear-gradient(135deg, #e6a23c 0%, #f0ad4e 50%, #f5c26b 100%) !important; }
.batch-result-dialog :deep(.el-dialog__body) { padding: 28px 32px !important; background: #f8f9fb; }
.batch-result-body { display: flex; flex-direction: column; gap: 24px; }
.result-summary { display: flex; justify-content: center; gap: 48px; }
.result-stat { display: flex; align-items: center; gap: 12px; padding: 16px 28px; border-radius: 14px; background: #ffffff; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.stat-text { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 28px; font-weight: bold; }
.success-stat .stat-num { color: #67c23a; }
.fail-stat .stat-num { color: #f56c6c; }
.stat-label { font-size: 12px; color: #999; margin-top: 2px; }
.result-detail-list { display: flex; flex-direction: column; gap: 8px; max-height: 220px; overflow-y: auto; }
.result-detail-title { font-size: 12px; color: #999; margin: 0 0 4px 0; }
.result-detail-item { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fff; border-radius: 8px; font-size: 13px; }
.result-detail-name { color: #333; font-weight: 500; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.result-detail-err { color: #f56c6c; font-size: 12px; margin-left: auto; }

.cover-upload-input { font-size: 12px; }
</style>