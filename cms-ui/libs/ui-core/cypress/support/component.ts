import { mount } from 'cypress/angular';
import { registerMount } from '../src';
import './commands';

registerMount(mount);
