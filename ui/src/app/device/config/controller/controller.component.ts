import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../shared/abstractconfig.component';
import { ConfigImpl } from '../../../shared/device/config';

@Component({
  selector: 'controller',
  templateUrl: '../shared/abstractconfig.component.html'
})
export class ControllerComponent extends AbstractConfigComponent {
  protected filterThings(config: ConfigImpl): string[] {
    return config.controllers;
  }
}