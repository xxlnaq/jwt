<script setup>

</script>

<template>
  <div style="width: 100vw;height: 100vh;overflow: hidden;display: flex">
   <div style="flex: 1 ;background-color: black" >
     <el-image style="width: 100%;height: 100%" fit="cover"
       src="https://tse4-mm.cn.bing.net/th/id/OIP-C.mzSuypCgv89SvAs9lmF4_wHaEK?w=283&h=180&c=7&r=0&o=5&dpr=2&pid=1.7">
     </el-image>
   </div>
    <div class="welcome-tile">
         <div style="font-size: 30px;font-weight: bold">vue的学习</div>
      <div style="margin-top:10px ">学vue电脑都快卡爆了</div>
      <div style="margin-top: 5px">现在是2025年4月1日晚上10点26分</div>
    </div>
    <div class="right-card">
    <router-view v-slot="{ Component }">
      <transition name="el-fade-in-linear" mode="out-in">
        <component :is="Component"/>
      </transition>
    </router-view>
<!--      路由-->
    </div>
  </div>
</template>

<style scoped>
.welcome-tile{
  position: absolute;
  bottom: 30px;
  left: 30px;
  color: antiquewhite;
  text-shadow: 0 0 10px black;
}
.right-card{
  width: 400px;
  z-index: 1;
  background-color:white ;
}
</style>