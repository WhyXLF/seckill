package org.seckill.web;

import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.dto.SeckillResult;
import org.seckill.entity.Seckill;
import org.seckill.enums.SeckillStatEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.awt.image.RescaleOp;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * Description:描述controller
 * User: ray.wang bookast@qq.com
 * Date: 16/5/22 下午6:53
 */
@Controller
@RequestMapping("/seckill")
public class SeckillController {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @RequestMapping(value = "list",method = RequestMethod.GET)
    public String list(Model model){
        model.addAttribute("list",seckillService.getSeckillList());
        return "list";
    }

    @RequestMapping(value = "{seckillId}/detail",method = RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Long seckillId,Model model){
        if (seckillId == null){
            return "redirect:/seckill/list";
        }
        Seckill seckill = seckillService.getById(seckillId);
        if(seckill == null){
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill",seckill);
        return "detail";
    }

    @RequestMapping(value = "{seckillId}/exposer",method = RequestMethod.POST,produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(Long seckillId){
        SeckillResult<Exposer> result;
        try{
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            result =  new SeckillResult<Exposer>(true,exposer);
        }catch (Exception e){
            LOG.error(e.getMessage(),e);
            result = new SeckillResult<Exposer>(false,e.getMessage());
        }
        return result;
    }

    /**
     * 秒杀执行方法.
     * @param seckillId 秒杀商品ID
     * @param userPhone 秒杀用户手机
     * @param md5 秒杀Key
     * @return
     */
    @RequestMapping(value = "{seckillId}/{md5}/execution",method = RequestMethod.POST,produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId")Long seckillId,
                                                   @CookieValue(value = "userPhone",required = false)Long userPhone,
                                                   @PathVariable("md5")String md5){
        SeckillResult<SeckillExecution> result;
        SeckillExecution seckillExecution;

        if (userPhone == null){
            result = new SeckillResult<SeckillExecution>(false,"未注册");
        }else {
            try{
                seckillExecution = seckillService.executeSeckill(seckillId, userPhone, md5);
                result =  new SeckillResult<SeckillExecution>(true,seckillExecution);
            }catch (SeckillCloseException e){
                seckillExecution = new SeckillExecution(seckillId, SeckillStatEnum.END);
                result = new SeckillResult<SeckillExecution>(false,seckillExecution);
            }catch (RepeatKillException e){
                seckillExecution = new SeckillExecution(seckillId, SeckillStatEnum.REPEAT_KILL);
                result = new SeckillResult<SeckillExecution>(false,seckillExecution);
            }catch (Exception e){
                LOG.error(e.getMessage(),e);
                seckillExecution = new SeckillExecution(seckillId, SeckillStatEnum.INNER_ERROR);
                result = new SeckillResult<SeckillExecution>(false,seckillExecution);
            }
        }

        return result;
    }

    @RequestMapping(value = "time/now",method = RequestMethod.GET,produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Long> execute(Model model) {
        return new SeckillResult<Long>(true,new Date().getTime());
    }
}
